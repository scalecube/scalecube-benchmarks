package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.benchmarks.metrics.BenchmarksTimer;
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
    BenchmarksSettings settings =
        BenchmarksSettings.from(args)
            .rampUpDuration(Duration.ofSeconds(10))
            .rampUpInterval(Duration.ofSeconds(1))
            .executionTaskDuration(Duration.ofSeconds(30))
            .executionTaskInterval(Duration.ZERO)
            .durationUnit(TimeUnit.NANOSECONDS)
            .build();

    new ExampleServiceBenchmarksState(settings)
        .runWithRampUp(
            (rampUpIteration, state) -> {
              if (rampUpIteration > 1) {
                return Mono.error(new RuntimeException("Exception instead of setUp result"));
              } else {
                return Mono.just(new ServiceCaller(state.exampleService()));
              }
            },
            state -> {
              BenchmarksTimer timer = state.timer("timer");
              return (iteration, serviceCaller) -> {
                BenchmarksTimer.Context timeContext = timer.time();
                return serviceCaller.call("hello").doOnTerminate(timeContext::stop);
              };
            },
            (state, serviceCaller) -> serviceCaller.close());
  }
}
