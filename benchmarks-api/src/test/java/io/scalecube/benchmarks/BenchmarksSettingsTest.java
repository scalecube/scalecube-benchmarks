package io.scalecube.benchmarks;

import static io.scalecube.benchmarks.BenchmarksSettings.MINIMAL_INTERVAL;
import static io.scalecube.benchmarks.BenchmarksSettings.N_THREADS;
import static io.scalecube.benchmarks.BenchmarksSettings.SCENARIO_DURATION;
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

    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(Duration.ZERO, settings.rampUpInterval());
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

  @Test
  void testDefaultSettings() {
    BenchmarksSettings settings = BenchmarksSettings.from(EMPTY_ARGS).build();

    assertEquals(Duration.ZERO, settings.rampUpInterval());
    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(N_THREADS, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithInjectors() {
    int injectors = 1000;
    BenchmarksSettings settings = BenchmarksSettings.from(EMPTY_ARGS).injectors(injectors).build();

    assertEquals(Duration.ZERO, settings.rampUpInterval());
    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(injectors, settings.injectors());
    assertEquals(injectors, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithMessageRate() {
    int messageRate = 1000;
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS).messageRate(messageRate).build();

    assertEquals(Duration.ZERO, settings.rampUpInterval());
    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(N_THREADS, settings.injectorsPerRampUpInterval());
    assertEquals(MINIMAL_INTERVAL, settings.executionTaskInterval());
    assertEquals(25, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(messageRate, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithExecutionTaskInterval() {
    Duration executionTaskInterval = Duration.ofSeconds(2);
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS).executionTaskInterval(executionTaskInterval).build();

    assertEquals(Duration.ZERO, settings.rampUpInterval());
    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(N_THREADS, settings.injectorsPerRampUpInterval());
    assertEquals(executionTaskInterval, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithScenarioDuration() {
    Duration scenarioDuration = Duration.ofMinutes(2);
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS).scenarioDuration(scenarioDuration).build();

    assertEquals(Duration.ZERO, settings.rampUpInterval());
    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(N_THREADS, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(scenarioDuration, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithRampUpDuration() {
    Duration rampUpDuration = Duration.ofSeconds(10);
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS).rampUpDuration(rampUpDuration).build();

    assertEquals(rampUpDuration, settings.rampUpDuration());
    assertEquals(rampUpDuration.dividedBy(N_THREADS), settings.rampUpInterval());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(1, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithRampUpInterval() {
    Duration rampUpInterval = Duration.ofMillis(200);
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS).rampUpInterval(rampUpInterval).build();

    assertEquals(Duration.ZERO, settings.rampUpDuration());
    assertEquals(Duration.ZERO, settings.rampUpInterval());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(N_THREADS, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithRampUpParams() {
    Duration rampUpInterval = Duration.ofMillis(200);
    Duration rampUpDuration = Duration.ofSeconds(20);
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS)
            .rampUpInterval(rampUpInterval)
            .rampUpDuration(rampUpDuration)
            .build();

    assertEquals(rampUpDuration, settings.rampUpDuration());
    assertEquals(rampUpDuration.dividedBy(N_THREADS), settings.rampUpInterval());
    assertEquals(N_THREADS, settings.injectors());
    assertEquals(1, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithRampUpParamsAndInjectors() {
    Duration rampUpInterval = Duration.ofMillis(200);
    Duration rampUpDuration = Duration.ofSeconds(20);
    int injectors = 300;
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS)
            .injectors(injectors)
            .rampUpInterval(rampUpInterval)
            .rampUpDuration(rampUpDuration)
            .build();

    assertEquals(rampUpDuration, settings.rampUpDuration());
    assertEquals(MINIMAL_INTERVAL, settings.rampUpInterval());
    assertEquals(injectors, settings.injectors());
    assertEquals(1, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }

  @Test
  void testDefaultSettingsWithRampUpParamsAndMoreInjectors() {
    Duration rampUpInterval = Duration.ofMillis(200);
    Duration rampUpDuration = Duration.ofSeconds(20);
    int injectors = 3000;
    long injectorsPerRampUpInterval =
        injectors / (rampUpDuration.toMillis() / MINIMAL_INTERVAL.toMillis());
    BenchmarksSettings settings =
        BenchmarksSettings.from(EMPTY_ARGS)
            .injectors(injectors)
            .rampUpInterval(rampUpInterval)
            .rampUpDuration(rampUpDuration)
            .build();

    assertEquals(rampUpDuration, settings.rampUpDuration());
    assertEquals(MINIMAL_INTERVAL, settings.rampUpInterval());
    assertEquals(injectors, settings.injectors());
    assertEquals(injectorsPerRampUpInterval, settings.injectorsPerRampUpInterval());
    assertEquals(Duration.ZERO, settings.executionTaskInterval());
    assertEquals(Integer.MAX_VALUE, settings.messagesPerExecutionInterval());
    assertEquals(SCENARIO_DURATION, settings.scenarioDuration());
    assertEquals(Integer.MAX_VALUE, settings.messageRate());
    assertEquals(Long.MAX_VALUE, settings.numOfIterations());
  }
}
