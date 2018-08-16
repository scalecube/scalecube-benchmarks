package io.scalecube.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BenchmarksSettingsTest {
  private static final String[] EMPTY_ARGS = new String[] {};

  @Test
  void testOneUserAndSevenMsgRate() {
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS)
            .injectors(1)
            .messageRate(7)
            .scenarioDuration(Duration.ofSeconds(10))
            .executionTaskInterval(Duration.ofSeconds(3))
            .build();

    assertEquals(Duration.ofSeconds(10), settings.rampUpDuration());
    assertEquals(Duration.ofSeconds(10), settings.rampUpInterval());
    assertEquals(Duration.ofMillis(100), settings.executionTaskInterval());
    assertEquals(Duration.ofSeconds(10), settings.scenarioDuration());
    assertEquals(1, settings.injectorsPerRampUpInterval());
    assertEquals(1, settings.messagesPerExecutionInterval());
  }

  @Test
  void testInjectorsCount() {
    // Given
    int injectors = 1000;

    // When
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS)
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
