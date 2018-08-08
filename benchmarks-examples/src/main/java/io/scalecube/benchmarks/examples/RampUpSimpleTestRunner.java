package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class RampUpSimpleTestRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .injectors(5)
        .messageRate(5)
        .rampUpDuration(Duration.ofSeconds(5))
        .executionTaskDuration(Duration.ofSeconds(10))
        .consoleReporterEnabled(false)
        .durationUnit(TimeUnit.NANOSECONDS)
        .build();

    System.out.println("Settings:");
    System.out.println(settings);
    System.out.println(LocalDateTime.now() + " Test started");

    new ExampleServiceBenchmarksState(settings).runWithRampUp(
        // set up
        (rampUpIteration, state) -> {
          System.out.println(LocalDateTime.now() + " User started: " + rampUpIteration);
          return Mono.just(rampUpIteration);
        },

        // job
        state -> (iteration, userId) -> {
          System.out.println(LocalDateTime.now() + " User: " + userId + " | iteration: " + iteration);
          return Mono.fromRunnable(RampUpSimpleTestRunner::heavy);
        },

        // teardown
        (state, userId) -> {
          System.out.println(LocalDateTime.now() + " User done:" + userId);
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
