package io.scalecube.benchmarks.examples;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.scalecube.benchmarks.BenchmarksSettings;
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
              Timer timer = state.timer("timer");
              Meter meter = state.meter("meter");
              Histogram histogram = state.histogram("histogram");

              return i -> {
                long start = System.nanoTime();
                Timer.Context timeContext = timer.time();
                return service
                    .invoke("hello")
                    .doOnTerminate(
                        () -> {
                          timeContext.stop();
                          meter.mark();
                          histogram.update(System.nanoTime() - start);
                        });
              };
            });
  }
}
