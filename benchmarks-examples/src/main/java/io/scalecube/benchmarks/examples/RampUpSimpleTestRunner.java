package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarkSettings;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class RampUpSimpleTestRunner {

  public static final Logger LOGGER = LoggerFactory.getLogger(RampUpSimpleTestRunner.class);

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarkSettings settings =
        BenchmarkSettings.from(args)
            .injectors(5)
            .messageRate(5)
            .rampUpDuration(Duration.ofSeconds(5))
            .executionTaskDuration(Duration.ofSeconds(10))
            .consoleReporterEnabled(false)
            .durationUnit(TimeUnit.NANOSECONDS)
            .build();

    new ExampleServiceBenchmarkState(settings)
        .runWithRampUp(
            // set up
            (rampUpIteration, state) -> {
              LOGGER.info("User started: " + rampUpIteration);
              return Mono.just(rampUpIteration);
            },

            // job
            state ->
                (iteration, userId) -> {
                  LOGGER.info("User: " + userId + " | iteration: " + iteration);
                  return Mono.fromRunnable(RampUpSimpleTestRunner::heavy);
                },

            // teardown
            (state, userId) -> {
              LOGGER.info("User done:" + userId);
              return Mono.empty();
            });
    System.out.println(LocalDateTime.now() + " Test over");
  }

  private static void heavy() {
    for (int i = 0; i < 100; i++) {
      // noinspection ResultOfMethodCallIgnored
      Math.hypot(20, 29 + i);
    }
  }
}
