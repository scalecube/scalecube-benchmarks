package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;
import io.scalecube.benchmarks.metrics.BenchmarkTimer.Context;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;

public class RampUpExampleFailingSetUpRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarkSettings settings =
        BenchmarkSettings.from(args)
            .rampUpDuration(Duration.ofSeconds(10))
            .rampUpInterval(Duration.ofSeconds(1))
            .executionTaskDuration(Duration.ofSeconds(30))
            .executionTaskInterval(Duration.ZERO)
            .durationUnit(TimeUnit.NANOSECONDS)
            .build();

    new ExampleServiceBenchmarkState(settings)
        .runWithRampUp(
            (rampUpIteration, state) -> {
              if (rampUpIteration > 1) {
                return Mono.error(new RuntimeException("Exception instead of setUp result"));
              } else {
                return Mono.just(new ServiceCaller(state.exampleService()));
              }
            },
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
