package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RampUpExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();

    AtomicInteger generator = new AtomicInteger();

    new ExampleServiceBenchmarksState(settings).run($ -> generator.incrementAndGet(),
        state -> {

          ExampleService service = state.exampleService();
          Timer timer = state.timer("timer");

          return (i, counter) -> {
            Timer.Context timeContext = timer.time();
            return service.invoke("hello" + counter)
                .doOnEach(System.err::println)
                .doOnTerminate(timeContext::stop);
          };
        });
  }
}
