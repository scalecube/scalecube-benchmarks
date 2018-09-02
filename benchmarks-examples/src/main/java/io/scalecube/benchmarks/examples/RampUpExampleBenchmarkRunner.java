package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;
import io.scalecube.benchmarks.metrics.BenchmarkTimer.Context;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;

public class RampUpExampleBenchmarkRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarkSettings settings =
        BenchmarkSettings.from(args)
            .rampUpDuration(Duration.ofSeconds(30))
            .injectors(10_000)
            .messageRate(50_000)
            .executionTaskDuration(Duration.ofSeconds(30))
            .durationUnit(TimeUnit.NANOSECONDS)
            .build();

    new ExampleServiceBenchmarkState(settings)
        .runWithRampUp(
            (rampUpIteration, state) -> Mono.just(new ServiceCaller(state.exampleService())),
            state -> {
              BenchmarkTimer timer = state.timer("timer");
              return (iteration, serviceCaller) -> {
                Context timeContext = timer.time();
                return serviceCaller.call("hello").doOnTerminate(timeContext::stop);
              };
            },
            (state, serviceCaller) -> serviceCaller.close());
  }
}
