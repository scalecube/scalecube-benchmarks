package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.benchmarks.metrics.BenchmarksMeter;
import io.scalecube.benchmarks.metrics.BenchmarksTimer;
import io.scalecube.benchmarks.metrics.BenchmarksTimer.Context;
import java.util.concurrent.TimeUnit;

public class ExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings =
        BenchmarksSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();
    new ExampleServiceBenchmarksState(settings)
        .runForAsync(
            state -> {
              ExampleService service = state.exampleService();
              BenchmarksTimer timer = state.timer("timer");
              BenchmarksMeter meter = state.meter("meter");

              return i -> {
                Context timeContext = timer.time();
                return service
                    .invoke("hello")
                    .doOnTerminate(
                        () -> {
                          timeContext.stop();
                          meter.mark();
                        });
              };
            });
  }
}
