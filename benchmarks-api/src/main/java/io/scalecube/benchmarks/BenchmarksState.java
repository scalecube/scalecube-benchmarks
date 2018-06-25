package io.scalecube.benchmarks;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
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

  private ConsoleReporter consoleReporter;
  private Scheduler scheduler;
  private CsvReporter csvReporter;

  private final AtomicBoolean started = new AtomicBoolean();

  public BenchmarksState(BenchmarksSettings settings) {
    this.settings = settings;
  }

  protected void beforeAll() {
    // NOP
  }

  protected void afterAll() {
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

    beforeAll();

    consoleReporter.start(settings.reporterPeriod().toMillis(), TimeUnit.MILLISECONDS);
    csvReporter.start(1, TimeUnit.DAYS);

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

    afterAll();
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
      Flux.merge(Flux.fromStream(LongStream.range(0, Long.MAX_VALUE).boxed())
          .publishOn(scheduler())
          .map(unitOfWork))
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
      Function<Long, Publisher<?>> publisherToBenchmarkMethod = func.apply(self);
      Flux.merge(Flux.fromStream(LongStream.range(0, Long.MAX_VALUE).boxed())
          .publishOn(scheduler())
          .map(publisherToBenchmarkMethod))
          .take(settings.executionTaskTime())
          .blockLast();
    } finally {
      self.shutdown();
    }
  }

}