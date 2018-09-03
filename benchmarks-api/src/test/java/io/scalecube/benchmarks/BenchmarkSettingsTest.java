package io.scalecube.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BenchmarkSettingsTest {
  private static final String[] EMPTY_ARGS = new String[] {};

  @Test
  void testOneUserAndSevenMsgRate() {
    BenchmarkSettings settings =
        BenchmarkSettings.from(EMPTY_ARGS)
            .injectors(1)
            .messageRate(7)
            .executionTaskDuration(Duration.ofSeconds(10))
            .executionTaskInterval(Duration.ofSeconds(3))
            .build();

    assertEquals(Duration.ofSeconds(10), settings.rampUpDuration());
    assertEquals(Duration.ofSeconds(10), settings.rampUpInterval());
    assertEquals(Duration.ofMillis(100), settings.executionTaskInterval());
    assertEquals(Duration.ofSeconds(10), settings.executionTaskDuration());
    assertEquals(1, settings.injectorsPerRampUpInterval());
    assertEquals(1, settings.messagesPerExecutionInterval());
  }

  @Test
  void testInjectorsCount() {
    // Given
    int injectors = 1000;

    // When
    BenchmarkSettings settings =
        BenchmarkSettings.from(EMPTY_ARGS)
            .injectors(injectors)
            .rampUpDuration(Duration.ofSeconds(20))
            .messageRate(10_000)
            .build();

    // Then:
    long intervals = settings.rampUpDuration().toMillis() / settings.rampUpInterval().toMillis();
    long resultInjectors = settings.injectorsPerRampUpInterval() * intervals;

    assertEquals(injectors, resultInjectors);
  }
}
