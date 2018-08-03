package io.scalecube.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class BenchmarksSettingsTest {
  private static final String[] EMPTY_ARGS = new String[] {};

  @Test
  void testOneUserAndSevenMsgRate() {
    BenchmarksSettings settings = BenchmarksSettings.from(EMPTY_ARGS)
        .users(1)
        .messageRate(7)
        .executionTaskDuration(Duration.ofSeconds(10))
        .executionTaskInterval(Duration.ofSeconds(3))
        .build();

    assertEquals(Duration.ofSeconds(10), settings.rampUpDuration());
    assertEquals(Duration.ofSeconds(10), settings.rampUpInterval());
    assertEquals(Duration.ofMillis(100), settings.executionTaskInterval());
    assertEquals(Duration.ofSeconds(10), settings.executionTaskDuration());
    assertEquals(1, settings.usersPerRampUpInterval());
    assertEquals(1, settings.messagesPerExecutionInterval());
  }
}
