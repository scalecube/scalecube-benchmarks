package io.scalecube.benchmarks;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * BenchmarksState is the state of the benchmark. it gives you the analogy of the beginning, and
 * ending of the test. It can run both sync or async way using the {@link #runForSync(Function)} and
 * {@link #runForAsync(Function)} respectively.
 *
 * @param <S> when extending this class, please add your class as the S. ie.
 *     <pre>{@code
 * public class ExampleBenchmarksState extends BenchmarksState<ExampleBenchmarksState> {
 *   ...
 * }
 * }</pre>
 */
public class BenchmarksState<S extends BenchmarksState<S>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksState.class);

  protected final BenchmarksSettings settings;

  private Scheduler scheduler;
  private List<Scheduler> schedulers;

  private ConsoleReporter consoleReporter;
  private CsvReporter csvReporter;

  private final AtomicBoolean started = new AtomicBoolean();

  public BenchmarksState(BenchmarksSettings settings) {
    this.settings = settings;
  }

  protected void beforeAll() throws Exception {
    // NOP
  }

  protected void afterAll() throws Exception {
    // NOP
  }

  /**
   * Executes starting of the state, also it includes running of {@link BenchmarksState#beforeAll}.
   */
  public final void start() {
    if (!started.compareAndSet(false, true)) {
      throw new IllegalStateException("BenchmarksState is already started");
    }

    LOGGER.info("Benchmarks settings: " + settings);

    if (settings.consoleReporterEnabled()) {
      consoleReporter =
          ConsoleReporter.forRegistry(settings.registry())
              .outputTo(System.out)
              .convertDurationsTo(settings.durationUnit())
              .convertRatesTo(settings.rateUnit())
              .build();
    }

    csvReporter =
        CsvReporter.forRegistry(settings.registry())
            .convertDurationsTo(settings.durationUnit())
            .convertRatesTo(settings.rateUnit())
            .build(settings.csvReporterDirectory());

    scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(settings.numberThreads()));

    schedulers =
        IntStream.rangeClosed(1, settings.numberThreads())
            .mapToObj(
                i -> Schedulers.fromExecutorService(Executors.newSingleThreadScheduledExecutor()))
            .collect(Collectors.toList());

    try {
      beforeAll();
    } catch (Exception ex) {
      throw new IllegalStateException("BenchmarksState beforeAll() failed: " + ex, ex);
    }

    settings.registry().register(settings.taskName() + "-gc", new GarbageCollectorMetricSet());
    settings.registry().register(settings.taskName() + "-memory", new MemoryUsageGaugeSet());
    settings.registry().register(settings.taskName() + "-threads", new ThreadStatesGaugeSet());

    if (settings.consoleReporterEnabled()) {
      consoleReporter.start(settings.reporterInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    csvReporter.start(settings.reporterInterval().toMillis(), TimeUnit.MILLISECONDS);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (started.get()) {
                    csvReporter.report();
                    if (consoleReporter != null) {
                      consoleReporter.report();
                    }
                  }
                }));
  }

  /**
   * Executes shutdown process of the state, also it includes running of {@link
   * BenchmarksState#afterAll}.
   */
  public final void shutdown() {
    if (!started.compareAndSet(true, false)) {
      throw new IllegalStateException("BenchmarksState is not started");
    }

    if (consoleReporter != null) {
      consoleReporter.report();
      consoleReporter.stop();
    }

    if (csvReporter != null) {
      csvReporter.report();
      csvReporter.stop();
    }

    if (scheduler != null) {
      scheduler.dispose();
    }

    if (schedulers != null) {
      schedulers.forEach(Scheduler::dispose);
    }

    try {
      afterAll();
    } catch (Exception ex) {
      throw new IllegalStateException("BenchmarksState afterAll() failed: " + ex, ex);
    }
  }

  public Scheduler scheduler() {
    return scheduler;
  }

  public List<Scheduler> schedulers() {
    return schedulers;
  }

  /**
   * Returns timer with specified name.
   *
   * @param name name
   * @return timer with specified name
   */
  public Timer timer(String name) {
    return settings.registry().timer(settings.taskName() + "-" + name);
  }

  /**
   * Returns meter with specified name.
   *
   * @param name name
   * @return meter with specified name
   */
  public Meter meter(String name) {
    return settings.registry().meter(settings.taskName() + "-" + name);
  }

  /**
   * Returns histogram with specified name.
   *
   * @param name name
   * @return histogram with specified name
   */
  public Histogram histogram(String name) {
    return settings.registry().histogram(settings.taskName() + "-" + name);
  }

  /**
   * Returns counter with specified name.
   *
   * @param name name
   * @return counter with specified name
   */
  public Counter counter(String name) {
    return settings.registry().counter(settings.taskName() + "-" + name);
  }

  /**
   * Runs given function in the state. It also executes {@link BenchmarksState#start()} before and
   * {@link BenchmarksState#shutdown()} after.
   *
   * <p>NOTICE: It's only for synchronous code.
   *
   * @param func a function that should return the execution to be tested for the given S. This
   *     execution would run on all positive values of Long (i.e. the benchmark itself) the return
   *     value is ignored.
   */
  public final void runForSync(Function<S, Function<Long, Object>> func) {
    @SuppressWarnings("unchecked")
    S self = (S) this;
    try {
      // noinspection unchecked
      self.start();

      Function<Long, Object> unitOfWork = func.apply(self);

      CountDownLatch latch = new CountDownLatch(1);

      Flux.fromStream(LongStream.range(0, settings.numOfIterations()).boxed())
          .parallel()
          .runOn(scheduler())
          .map(unitOfWork)
          .doOnTerminate(latch::countDown)
          .subscribe();

      latch.await(settings.scenarioDuration().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw Exceptions.propagate(e);
    } finally {
      self.shutdown();
    }
  }

  /**
   * Runs given function on this state. It also executes {@link BenchmarksState#start()} before and
   * {@link BenchmarksState#shutdown()} after.
   *
   * <p>NOTICE: It's only for asynchronous code.
   *
   * @param func a function that should return the execution to be tested for the given S. This
   *     execution would run on all positive values of Long (i.e. the benchmark itself) On the
   *     return value, as it is a Publisher, The benchmark test would {@link
   *     Publisher#subscribe(Subscriber) subscribe}, And upon all subscriptions - await for
   *     termination.
   */
  public final void runForAsync(Function<S, Function<Long, Publisher<?>>> func) {
    // noinspection unchecked
    @SuppressWarnings("unchecked")
    S self = (S) this;
    try {
      self.start();

      Function<Long, Publisher<?>> unitOfWork = func.apply(self);

      Flux<Long> fromStream =
          Flux.fromStream(LongStream.range(0, settings.numOfIterations()).boxed());

      Flux.merge(fromStream.publishOn(scheduler()).map(unitOfWork))
          .take(settings.scenarioDuration())
          .blockLast();
    } finally {
      self.shutdown();
    }
  }

  /**
   * Uses a resource, generated by a supplier, to run given function on this activated state. The
   * Publisher of resources will be received from the setUp function. The setUp function will be
   * invoked with given intervals, according to the benchmark rampUp settings. And when the resource
   * will be available then it will start executing the unitOfWork. And when execution of the
   * unitOfWorks will be finished (by time completion or by iteration's completion) the resource
   * will be released with the cleanUp function. It also executes {@link BenchmarksState#start()}
   * before and {@link BenchmarksState#shutdown()} after.
   *
   * @param setUp a function that should return some T type which one will be passed into next to
   *     the argument. Also, this function will be invoked with some ramp-up strategy, and when it
   *     will be invoked it will start executing the unitOfWork, which one specified as the second
   *     argument of this method.
   * @param func a function that should return the execution to be tested for the given S. This
   *     execution would run on all positive values of Long (i.e. the benchmark itself) On the
   *     return value, as it is a Publisher, The benchmark test would {@link
   *     Publisher#subscribe(Subscriber) subscribe}, And upon all subscriptions - await for
   *     termination.
   * @param cleanUp a function that should clean up some T's resources.
   */
  public final <T> void runWithRampUp(
      BiFunction<Long, S, Publisher<T>> setUp,
      Function<S, BiFunction<Long, T, Publisher<?>>> func,
      BiFunction<S, T, Mono<Void>> cleanUp) {

    // noinspection unchecked
    @SuppressWarnings("unchecked")
    S self = (S) this;
    try {
      self.start();

      BiFunction<Long, T, Publisher<?>> unitOfWork = func.apply(self);

      Flux.interval(Duration.ZERO, settings.rampUpInterval())
          .take(settings.rampUpDuration())
          .flatMap(
              rampUpIteration -> {

                // select scheduler and bind tasks to it
                int schedulerIndex =
                    (int) ((rampUpIteration & Long.MAX_VALUE) % schedulers().size());
                Scheduler scheduler = schedulers().get(schedulerIndex);

                return Flux.range(0, Math.max(1, settings.injectorsPerRampUpInterval()))
                    .flatMap(
                        iteration1 -> {
                          // create tasks on selected scheduler
                          Flux<T> setUpFactory =
                              Flux.create(
                                  (FluxSink<T> sink) -> {
                                    Flux<T> deferSetUp =
                                        Flux.defer(() -> setUp.apply(rampUpIteration, self));
                                    deferSetUp.subscribe(
                                        sink::next,
                                        ex -> {
                                          LOGGER.error(
                                              "Exception occured on setUp at rampUpIteration: {}, "
                                                  + "cause: {}, task won't start",
                                              rampUpIteration,
                                              ex);
                                          sink.complete();
                                        },
                                        sink::complete);
                                  });
                          return setUpFactory
                              .subscribeOn(scheduler)
                              .map(
                                  setUpResult ->
                                      new BenchmarksTask<>(
                                          self, setUpResult, unitOfWork, cleanUp, scheduler))
                              .doOnNext(scheduler::schedule)
                              .flatMap(BenchmarksTask::completionMono);
                        });
              },
              Integer.MAX_VALUE,
              Integer.MAX_VALUE)
          .blockLast();
    } finally {
      self.shutdown();
    }
  }
}
