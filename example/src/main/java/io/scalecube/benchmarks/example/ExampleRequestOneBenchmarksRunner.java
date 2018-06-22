package io.scalecube.benchmarks.example;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

public class ExampleRequestOneBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args).build();
    new ExampleServicesBenchmarksState(settings, new ExampleBenchmarkServiceImpl()).runForAsync(state -> {

      ExampleBenchmarkService exampleBenchmarkService = state.service(ExampleBenchmarkService.class);
      Timer timer = state.timer("timer");

      return i -> {
        Timer.Context timeContext = timer.time();
        return exampleBenchmarkService.requestOne("hello").doOnTerminate(timeContext::stop);
      };
    });
  }
}
