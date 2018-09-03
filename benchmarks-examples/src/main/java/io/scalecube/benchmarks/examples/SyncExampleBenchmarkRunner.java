package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.benchmarks.metrics.BenchmarkMeter;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;
import io.scalecube.benchmarks.metrics.BenchmarkTimer.Context;
import java.util.concurrent.TimeUnit;

public class SyncExampleBenchmarkRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarkSettings settings =
        BenchmarkSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();
    new ExampleServiceBenchmarkState(settings)
        .runForSync(
            state -> {
              ExampleService service = state.exampleService();
              BenchmarkTimer timer = state.timer("timer");
              BenchmarkMeter meter = state.meter("meter");

              return i -> {
                Context timeContext = timer.time();
                String result = service.syncInvoke("hello");
                timeContext.stop();
                meter.mark();
                return result;
              };
            });
  }
}
