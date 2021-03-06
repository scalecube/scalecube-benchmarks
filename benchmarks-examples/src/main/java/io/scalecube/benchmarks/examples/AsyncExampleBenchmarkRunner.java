package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.benchmarks.metrics.BenchmarkMeter;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;
import io.scalecube.benchmarks.metrics.BenchmarkTimer.Context;
import java.util.concurrent.TimeUnit;

public class AsyncExampleBenchmarkRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarkSettings settings =
        BenchmarkSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();
    new ExampleServiceBenchmarkState(settings)
        .runForAsync(
            state -> {
              ExampleService service = state.exampleService();
              BenchmarkTimer timer = state.timer("timer");
              BenchmarkMeter meter = state.meter("meter");

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
