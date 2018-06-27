package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

public class TimerExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args).durationUnit(TimeUnit.NANOSECONDS).build();
    new ExampleServicesBenchmarksState(settings).runForAsync(state -> {

      ExampleService service = state.exampleService();
      Timer timer = state.timer("timer");

      return i -> {
        Timer.Context timeContext = timer.time();
        return service.invoke("hello").doOnTerminate(timeContext::stop);
      };
    });
  }
}
