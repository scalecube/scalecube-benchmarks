package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Meter;

import java.util.concurrent.TimeUnit;

public class MeterExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();
    new ExampleServicesBenchmarksState(settings).runForAsync(state -> {

      ExampleService service = state.exampleService();
      Meter meter = state.meter("meter");

      return i -> service.invoke("hello").doOnTerminate(meter::mark);
    });
  }
}
