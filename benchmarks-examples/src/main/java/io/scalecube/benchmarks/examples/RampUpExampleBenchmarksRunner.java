package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RampUpExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .rampUpDuration(Duration.ofSeconds(10))
        .executionTaskDuration(Duration.ofSeconds(30))
        .durationUnit(TimeUnit.NANOSECONDS)
        .build();

    new ExampleServiceBenchmarksState(settings).runWithRampUp(
        (rampUpIteration, state) -> Mono.just(new ServiceCaller(state.exampleService())),
        state -> {
          Timer timer = state.timer("timer");
          return (iteration, serviceCaller) -> {
            Timer.Context timeContext = timer.time();
            return serviceCaller.call("hello").doOnTerminate(timeContext::stop);
          };
        },
        (state, serviceCaller) -> serviceCaller.close());
  }
}
