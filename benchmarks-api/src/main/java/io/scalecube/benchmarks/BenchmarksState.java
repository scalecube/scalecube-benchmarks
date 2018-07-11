package io.scalecube.benchmarks;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;


/**
 * BenchmarksState is the state of the benchmark. it gives you the analogy of the beginning, and ending of the test. It
 * can run both sync or async way using the {@link #runForSync(Function)} and {@link #runForAsync(Function)}
 * respectively.
 * 
 * @param <SELF> when extending this class, please add your class as the SELF. ie.
 * 
 *        <pre>
 * {@code   
 *  public class ExampleBenchmarksState extends BenchmarksState<ExampleBenchmarksState> {   
 *    ...   
 *  }   
 * }
 *        </pre>
 */
public class BenchmarksState<SELF extends BenchmarksState<SELF>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksState.class);

  protected final BenchmarksSettings settings;

  protected Scheduler scheduler;
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

    settings.registry().register(settings.taskName() + "-memory", new MemoryUsageGaugeSet());

    consoleReporter = ConsoleReporter.forRegistry(settings.registry())
        .outputTo(System.out)
        .convertDurationsTo(settings.durationUnit())
        .convertRatesTo(settings.rateUnit())
        .build();

    csvReporter = CsvReporter.forRegistry(settings.registry())
        .convertDurationsTo(settings.durationUnit())
        .convertRatesTo(settings.rateUnit())
        .build(settings.csvReporterDirectory());

    scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(settings.nThreads()));

    try {
      beforeAll();
    } catch (Exception ex) {
      throw new IllegalStateException("BenchmarksState beforeAll() failed: " + ex, ex);
    }

    consoleReporter.start(settings.reporterPeriod().toMillis(), TimeUnit.MILLISECONDS);
    csvReporter.start(settings.reporterPeriod().toMillis(), TimeUnit.MILLISECONDS);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (started.get()) {
        csvReporter.report();
        consoleReporter.report();
      }
    }));

  }

  /**
   * Executes shutdown process of the state, also it includes running of {@link BenchmarksState#afterAll}.
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

    try {
      afterAll();
    } catch (Exception ex) {
      throw new IllegalStateException("BenchmarksState afterAll() failed: " + ex, ex);
    }
  }

  public Scheduler scheduler() {
    return scheduler;
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
   * Runs given function in the state. It also executes {@link BenchmarksState#start()} before and
   * {@link BenchmarksState#shutdown()} after.
   * <p>
   * NOTICE: It's only for synchronous code.
   * </p>
   *
   * @param func a function that should return the execution to be tested for the given SELF. This execution would run
   *        on all positive values of Long (i.e. the benchmark itself) the return value is ignored.
   */
  public final void runForSync(Function<SELF, Function<Long, Object>> func) {
    @SuppressWarnings("unchecked")
    SELF self = (SELF) this;
    try {
      // noinspection unchecked
      self.start();

      Function<Long, Object> unitOfWork = func.apply(self);

      Flux<Long> fromStream = Flux.fromStream(LongStream.range(0, settings.numOfIterations()).boxed());

      Flux.merge(fromStream
          .publishOn(scheduler()).map(unitOfWork))
          .take(settings.executionTaskTime())
          .blockLast();
    } finally {
      self.shutdown();
    }
  }

  /**
   * Runs given function on this state. It also executes {@link BenchmarksState#start()} before and
   * {@link BenchmarksState#shutdown()} after.
   * <p>
   * NOTICE: It's only for asynchronous code.
   * </p>
   *
   * @param func a function that should return the execution to be tested for the given SELF. This execution would run
   *        on all positive values of Long (i.e. the benchmark itself) On the return value, as it is a Publisher, The
   *        benchmark test would {@link Publisher#subscribe(Subscriber) subscribe}, And upon all subscriptions - await
   *        for termination.
   */
  public final void runForAsync(Function<SELF, Function<Long, Publisher<?>>> func) {
    // noinspection unchecked
    @SuppressWarnings("unchecked")
    SELF self = (SELF) this;
    try {
      self.start();

      Function<Long, Publisher<?>> unitOfWork = func.apply(self);

      Flux<Long> fromStream = Flux.fromStream(LongStream.range(0, settings.numOfIterations()).boxed());

      Flux.merge(fromStream.publishOn(scheduler()).map(unitOfWork))
          .take(settings.executionTaskTime())
          .blockLast();
    } finally {
      self.shutdown();
    }
  }

  /**
   * Runs given function on this state. It also executes {@link BenchmarksState#start()} before and
   * {@link BenchmarksState#shutdown()} after.
   * <p>
   * NOTICE: It's only for asynchronous code.
   * </p>
   *
   * @param supplier a function that should return some T type which one will be passed into next to the argument. Also,
   *        this function will be invoked with some ramp-up strategy, and when it will be invoked it will start
   *        executing the unitOfWork, which one specified as the second argument of this method.
   * @param func a function that should return the execution to be tested for the given SELF. This execution would run
   *        on all positive values of Long (i.e. the benchmark itself) On the return value, as it is a Publisher, The
   *        benchmark test would {@link Publisher#subscribe(Subscriber) subscribe}, And upon all subscriptions - await
   *        for termination.
   * @param cleanUp a function that should clean up some T's resources.
   */
  public final <T> void runForAsync(Function<SELF, Mono<T>> supplier,
      Function<SELF, BiFunction<Long, T, Publisher<?>>> func, Function<T, Mono<Void>> cleanUp) {

    // noinspection unchecked
    @SuppressWarnings("unchecked")
    SELF self = (SELF) this;
    try {
      self.start();

      BiFunction<Long, T, Publisher<?>> unitOfWork = func.apply(self);

      long unitOfWorkNumber = settings.numOfIterations();
      Duration unitOfWorkDuration = settings.executionTaskTime();

      List<Scheduler> schedulers = IntStream.rangeClosed(1, Runtime.getRuntime().availableProcessors())
          .mapToObj(i -> Schedulers.fromExecutorService(Executors.newSingleThreadScheduledExecutor()))
          .collect(Collectors.toList());

      Flux.interval(settings.rampUpDurationPerSupplier())
          .take(settings.rampUpAllDuration())
          .doOnNext(aLong -> {
            int i = (int) ((aLong & Long.MAX_VALUE) % schedulers.size());
            new MyRunnable<>(supplier, unitOfWork, schedulers.get(i)).run();
          })
          .blockLast();

      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // Flux.merge(Flux.interval(settings.rampUpDurationPerSupplier())
      // .take(settings.rampUpAllDuration())
      // .flatMap(i -> supplier.apply(self))
      // .flatMap(supplier0 -> Flux.fromStream(LongStream.range(0, unitOfWorkNumber).boxed())
      // .publishOn(scheduler)
      // .map(i -> unitOfWork.apply(i, supplier0))
      // .take(unitOfWorkDuration)
      // .doFinally($ -> cleanUp.apply(supplier0).subscribe())))
      // .blockLast();

    } finally {
      self.shutdown();
    }
  }

  private class MyRunnable<T> implements Runnable {
    private static final int NOT_STARTED = 0;
    private static final int STARTED = 1;
    private static final int SCHEDULED = 2;
    private static final int ERROR = 3;
    private static final int FINISH = 4;

    // public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final Function<SELF, Mono<T>> supplier;
    private final BiFunction<Long, T, Publisher<?>> unitOfWork;

    private final AtomicInteger state = new AtomicInteger();
    private final AtomicReference<T> resultReference = new AtomicReference<>();
    private final AtomicLong iterations = new AtomicLong();
    private final Scheduler scheduler;

    public MyRunnable(Function<SELF, Mono<T>> supplier,
        BiFunction<Long, T, Publisher<?>> unitOfWork,
        Scheduler scheduler) {

      this.supplier = supplier;
      this.unitOfWork = unitOfWork;
      this.scheduler = scheduler;
    }

    @Override
    public void run() {
      if (state.compareAndSet(NOT_STARTED, STARTED)) { // started
        // noinspection unchecked
        Mono<T> supplierMono = supplier.apply((SELF) BenchmarksState.this);
        supplierMono.subscribe(
            result -> {
              // netty epoll thread - 0
              if (state.compareAndSet(STARTED, SCHEDULED)) { // scheduled
                resultReference.set(result);
                scheduler.schedule(this);
                scheduler.schedule(() -> state.set(FINISH), settings.executionTaskTime().toMillis(),
                    TimeUnit.MILLISECONDS);
              }
            },
            throwable -> {
              state.set(ERROR);
            },
            () -> {
              // byte byte
            });
      }
      if (state.get() == SCHEDULED) { // executing
        long i = iterations.incrementAndGet();
        T t = resultReference.get();

        Flux.from(unitOfWork.apply(i, t))
            .doOnError(ex -> {
              state.set(ERROR);
              ex.printStackTrace();
            })
            .subscribe();

        scheduler.schedule(this, 0, TimeUnit.MILLISECONDS);
      }
    }
  }
}
