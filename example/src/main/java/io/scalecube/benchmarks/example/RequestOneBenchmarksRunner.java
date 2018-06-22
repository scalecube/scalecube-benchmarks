package io.scalecube.benchmarks.example;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

public class RequestOneBenchmarksRunner {

  /**
   * Runs benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args).build();
    new ServicesBenchmarksState(settings, new BenchmarkServiceImpl()).runForAsync(state -> {

      BenchmarkService benchmarkService = state.service(BenchmarkService.class);
      Timer timer = state.timer("timer");

      return i -> {
        Timer.Context timeContext = timer.time();
        return benchmarkService.requestOne("hello").doOnTerminate(timeContext::stop);
      };
    });
  }
}
