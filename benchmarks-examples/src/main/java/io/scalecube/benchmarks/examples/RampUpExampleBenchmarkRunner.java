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
            .injectors(1)
            .messageRate(1)
            .executionTaskDuration(Duration.ofSeconds(180))
            .durationUnit(TimeUnit.NANOSECONDS)
            .build();

    new ExampleServiceBenchmarkState(settings)
        .runWithRampUp(
            (rampUpIteration, state) -> Mono.just(new ServiceCaller(state.exampleService())),
            state -> {
              BenchmarkTimer timer = state.timer("timer");
              return serviceCaller ->
                  (i, task) -> {
                    Context timeContext = timer.time();
                    return serviceCaller
                        .call("hello")
                        .doOnTerminate(timeContext::stop)
                        .doOnTerminate(task::scheduleWithInterval);
                  };
            },
            (state, serviceCaller) -> serviceCaller.close());
  }
}
