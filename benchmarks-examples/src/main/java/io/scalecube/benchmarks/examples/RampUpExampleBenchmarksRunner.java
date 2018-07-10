package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RampUpExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .numOfIterations(10000000)
        .executionTaskTime(Duration.ofSeconds(5))
        .rampUpAllDuration(Duration.ofSeconds(10))
        .rampUpDurationPerSupplier(Duration.ofSeconds(1))
        .durationUnit(TimeUnit.NANOSECONDS)
        .build();

    AtomicInteger generator = new AtomicInteger();

    new ExampleServiceBenchmarksState(settings).runForAsync($ -> generator.incrementAndGet(),
        state -> {
          ExampleService service = state.exampleService();
          Timer timer = state.timer("timer");

          return (ignored, counter) -> {
            Timer.Context timeContext = timer.time();
            return service.invoke("hello" + counter)
                .doOnTerminate(timeContext::stop);
          };
        });
  }
}
