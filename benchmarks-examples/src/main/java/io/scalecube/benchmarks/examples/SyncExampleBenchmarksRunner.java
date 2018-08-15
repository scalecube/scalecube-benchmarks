package io.scalecube.benchmarks.examples;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.scalecube.benchmarks.BenchmarksSettings;
import java.util.concurrent.TimeUnit;

public class SyncExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings =
        BenchmarksSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();
    new ExampleServiceBenchmarksState(settings)
        .runForSync(
            state -> {
              ExampleService service = state.exampleService();
              Timer timer = state.timer("timer");
              Meter meter = state.meter("meter");

              return i -> {
                Timer.Context timeContext = timer.time();
                String result = service.syncInvoke("hello");
                timeContext.stop();
                meter.mark();
                return result;
              };
            });
  }
}
