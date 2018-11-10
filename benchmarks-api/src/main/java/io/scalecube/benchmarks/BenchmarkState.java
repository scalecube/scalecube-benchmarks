package io.scalecube.benchmarks;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import io.scalecube.benchmarks.metrics.BenchmarkMeter;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;
import io.scalecube.benchmarks.metrics.CodahaleBenchmarkMetrics;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * BenchmarkState is the state of the benchmark. it gives you the analogy of the beginning, and
 * ending of the test. It can run both sync or async way using the {@link #runForSync(Function)} and
 * {@link #runForAsync(Function)} respectively.
 *
 * @param <S> when extending this class, please add your class as the S. ie.
 *     <pre>{@code
 * public class ExampleBenchmarksState extends BenchmarkState<ExampleBenchmarksState> {
 *   ...
 * }
 * }</pre>
 */
public class BenchmarkState<S extends BenchmarkState<S>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkState.class);

  protected final BenchmarkSettings settings;

  private Scheduler scheduler;
  private List<Scheduler> schedulers;

  private MetricRegistry registry;
  private BenchmarkMetrics metrics;

  private ConsoleReporter consoleReporter;
  private CsvReporter csvReporter;

  private final AtomicBoolean started = new AtomicBoolean();

  private final AtomicBoolean warmUpOccurred = new AtomicBoolean(false);
  private Disposable warmUpSubscriber;

  public BenchmarkState(BenchmarkSettings settings) {
    this.settings = settings;
  }

  protected void beforeAll() throws Exception {
    // NOP
  }

  protected void afterAll() throws Exception {
    // NOP
  }

  /**
   * Executes starting of the state, also it includes running of {@link BenchmarkState#beforeAll}.
   */
  public final void start() {
    if (!started.compareAndSet(false, true)) {
      throw new IllegalStateException("BenchmarkState is already started");
    }

    LOGGER.info("Benchmarks settings: " + settings);

    registry = new MetricRegistry();

    metrics = new CodahaleBenchmarkMetrics(registry, warmUpOccurred::get);

    if (settings.consoleReporterEnabled()) {
      consoleReporter =
          ConsoleReporter.forRegistry(registry)
              .outputTo(System.out)
              .convertDurationsTo(settings.durationUnit())
              .convertRatesTo(settings.rateUnit())
              .build();
    }

    csvReporter =
        CsvReporter.forRegistry(registry)
            .convertDurationsTo(settings.durationUnit())
            .convertRatesTo(settings.rateUnit())
            .build(settings.csvReporterDirectory());

    scheduler = Schedulers.newParallel("par-scheduler", settings.numberThreads(), true);

    schedulers =
        IntStream.rangeClosed(1, settings.numberThreads())
            .mapToObj(i -> Schedulers.newSingle("single-scheduler", true))
            .collect(Collectors.toList());

    try {
      beforeAll();
    } catch (Exception ex) {
      throw new IllegalStateException("BenchmarkState beforeAll() failed: " + ex, ex);
    }

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

    warmUpSubscriber =
        Mono.delay(settings.warmUpDuration())
            .doOnSuccess(
                avoid -> {
                  warmUpOccurred.compareAndSet(false, true);
                  if (settings.consoleReporterEnabled()) {
                    consoleReporter.start(
                        settings.reporterInterval().toMillis(), TimeUnit.MILLISECONDS);
                  }
                  csvReporter.start(settings.reporterInterval().toMillis(), TimeUnit.MILLISECONDS);
                })
            .subscribe();
  }

  /**
   * Executes shutdown process of the state, also it includes running of {@link
   * BenchmarkState#afterAll}.
   */
  public final void shutdown() {
    if (!started.compareAndSet(true, false)) {
      throw new IllegalStateException("BenchmarkState is not started");
    }
    LOGGER.info("Benchmarks is shutdown...");

    if (warmUpSubscriber != null) {
      warmUpSubscriber.dispose();
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
      throw new IllegalStateException("BenchmarkState afterAll() failed: " + ex, ex);
    }
  }

  public Scheduler scheduler() {
    return scheduler;
  }

  public List<Scheduler> schedulers() {
    return schedulers;
  }

  public MetricRegistry registry() {
    return registry;
  }

  /**
   * Returns timer with specified name.
   *
   * @param name name
   * @return timer with specified name
   */
  public BenchmarkTimer timer(String name) {
    return metrics.timer(settings.taskName() + "-" + name);
  }

  /**
   * Returns meter with specified name.
   *
   * @param name name
   * @return meter with specified name
   */
  public BenchmarkMeter meter(String name) {
    return metrics.meter(settings.taskName() + "-" + name);
  }

  /**
   * Runs given function in the state. It also executes {@link BenchmarkState#start()} before and
   * {@link BenchmarkState#shutdown()} after.
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

      Function<Long, Object> unitOfWork = wrap(func.apply(self));

      CountDownLatch latch = new CountDownLatch(1);

      Disposable disposable =
          Flux.fromStream(LongStream.range(0, settings.numOfIterations()).boxed())
              .parallel()
              .runOn(scheduler())
              .map(unitOfWork)
              .doOnTerminate(latch::countDown)
              .subscribe();

      latch.await(settings.executionTaskDuration().toMillis(), TimeUnit.MILLISECONDS);
      disposable.dispose();
    } catch (InterruptedException e) {
      throw Exceptions.propagate(e);
    } finally {
      self.shutdown();
    }
  }

  /**
   * Runs given function on this state. It also executes {@link BenchmarkState#start()} before and
   * {@link BenchmarkState#shutdown()} after.
   *
   * <p>NOTICE: It's only for asynchronous code.
   *
   * @param func a function that should return the execution to be tested for the given S.
   */
  public final void runForAsync(Function<S, Function<Long, Publisher<?>>> func) {
    // noinspection unchecked
    @SuppressWarnings("unchecked")
    S self = (S) this;
    try {
      self.start();

      Function<Long, Publisher<?>> unitOfWork = wrap(func.apply(self));
      int threads = settings.numberThreads();
      long countPerThread = settings.numOfIterations() / threads;

      Function<Integer, Mono<?>> scenarioPerThread =
          i ->
              Mono.fromRunnable(
                  () -> {
                    long start = i * countPerThread;
                    Flux.fromStream(LongStream.range(start, start + countPerThread).boxed())
                        .flatMap(unitOfWork::apply, settings.concurrency(), Integer.MAX_VALUE)
                        .take(settings.executionTaskDuration())
                        .blockLast();
                  });

      Flux.range(0, threads)
          .flatMap(
              i -> scenarioPerThread.apply(i).subscribeOn(scheduler()),
              Integer.MAX_VALUE,
              Integer.MAX_VALUE)
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
   * will be released with the cleanUp function. It also executes {@link BenchmarkState#start()}
   * before and {@link BenchmarkState#shutdown()} after.
   *
   * @param setUp a function that should return some T type which one will be passed into next to
   *     the argument. Also, this function will be invoked with some ramp-up strategy, and when it
   *     will be invoked it will start executing the unitOfWork, which one specified as the second
   *     argument of this method.
   * @param func a function that should return the execution to be tested for the given S.
   * @param cleanUp a function that should clean up some T's resources.
   */
  public final <T> void runWithRampUp(
      BiFunction<Long, S, Publisher<T>> setUp,
      Function<S, Function<T, BiFunction<Long, BenchmarkTask, Publisher<?>>>> func,
      BiFunction<S, T, Mono<Void>> cleanUp) {

    // noinspection unchecked
    @SuppressWarnings("unchecked")
    S self = (S) this;
    try {
      self.start();

      Function<T, BiFunction<Long, BenchmarkTask, Publisher<?>>> unitOfWork =
          wrap(func.apply(self));

      Flux.interval(Duration.ZERO, settings.rampUpInterval())
          .take(settings.rampUpDuration())
          .flatMap(
              rampUpIteration -> {
                Scheduler scheduler = selectScheduler(rampUpIteration);
                return Flux.range(0, Math.max(1, settings.injectorsPerRampUpInterval()))
                    .flatMap(
                        iteration1 ->
                            createSetUpFactory(setUp, self, rampUpIteration)
                                .subscribeOn(scheduler)
                                .map(
                                    setUpResult -> {
                                      BiFunction<Long, BenchmarkTask, Publisher<?>> unitOfWork1 =
                                          unitOfWork.apply(setUpResult);

                                      Supplier<Mono<Void>> cleanUp1 =
                                          () -> cleanUp.apply(self, setUpResult);

                                      return new BenchmarkTaskImpl(
                                          self.settings, scheduler, unitOfWork1, cleanUp1);
                                    })
                                .doOnNext(scheduler::schedule)
                                .flatMap(BenchmarkTaskImpl::completionMono));
              },
              Integer.MAX_VALUE,
              Integer.MAX_VALUE)
          .blockLast();
    } finally {
      self.shutdown();
    }
  }

  private Scheduler selectScheduler(Long rampUpIteration) {
    // select scheduler and bind tasks to it
    int schedulerIndex = (int) ((rampUpIteration & Long.MAX_VALUE) % schedulers().size());
    return schedulers().get(schedulerIndex);
  }

  private <T> Flux<T> createSetUpFactory(
      BiFunction<Long, S, Publisher<T>> setUp, S self, Long rampUpIteration) {
    // create tasks on selected scheduler
    return Flux.create(
        (FluxSink<T> sink) -> {
          Flux<T> deferSetUp = Flux.defer(() -> setUp.apply(rampUpIteration, self));
          deferSetUp.subscribe(
              sink::next,
              ex -> {
                LOGGER.error(
                    "Exception occured on setUp at rampUpIteration: {}, "
                        + "cause: {}, task won't start",
                    rampUpIteration,
                    ex,
                    ex);
                sink.complete();
              },
              sink::complete);
        });
  }

  private <T, R> Function<T, R> wrap(Function<T, R> unitOfWork) {
    return i -> {
      try {
        return unitOfWork.apply(i);
      } catch (Exception e) {
        LOGGER.error("Occurred exception into unitOfWork: {}", e);
        throw Exceptions.propagate(e);
      }
    };
  }
}
