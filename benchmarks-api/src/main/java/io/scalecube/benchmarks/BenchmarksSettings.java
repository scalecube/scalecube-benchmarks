package io.scalecube.benchmarks;

import com.codahale.metrics.MetricRegistry;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class BenchmarksSettings {

  private static final int N_THREADS = Runtime.getRuntime().availableProcessors();
  private static final Duration TEST_DURATION = Duration.ofSeconds(60);
  private static final Duration REPORTER_PERIOD = Duration.ofSeconds(3);
  private static final TimeUnit REPORTER_DURATION_UNIT = TimeUnit.MILLISECONDS;
  private static final TimeUnit REPORTER_RATE_UNIT = TimeUnit.SECONDS;
  private static final long TASKS_COUNT = Long.MAX_VALUE;
  private static final Duration RAMP_UP_DURATION = Duration.ofSeconds(10);
  private static final Duration RAMP_UP_INTERVAL = Duration.ofSeconds(1);
  public static final int RAMP_UP_ITERATIONS = 100;

  private final int nThreads;
  private final Duration reporterPeriod;
  private final TimeUnit reporterDurationUnit;
  private final TimeUnit reporterRateUnit;
  private final File reporterCsvDirectory;
  private final String testName;
  private final MetricRegistry registry;
  private final Duration rampUpDuration;
  private final Duration rampUpInterval;
  private final long rampUpIterations;
  private final Duration taskInterval;
  private final Duration testDuration;
  private final long tasksCount;

  private final Map<String, String> options;

  public static Builder from(String[] args) {
    return new Builder().from(args);
  }

  private BenchmarksSettings(Builder builder) {
    this.nThreads = builder.nThreads;
    this.reporterPeriod = builder.reporterPeriod;
    this.rampUpDuration = builder.rampUpDuration;
    this.rampUpInterval = builder.rampUpInterval;
    this.rampUpIterations = builder.rampUpIterations;

    this.taskInterval = builder.taskInterval;
    this.testDuration = builder.testDuration;
    this.tasksCount = builder.tasksCount;

    this.options = builder.options;

    this.registry = new MetricRegistry();

    this.reporterDurationUnit = builder.reporterDurationUnit;
    this.reporterRateUnit = builder.reporterRateUnit;

    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    this.testName = minifyClassName(stackTrace[stackTrace.length - 1].getClassName());

    String time = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    this.reporterCsvDirectory = Paths.get("benchmarks", "results", testName + allPropertiesAsString(), time).toFile();
    // noinspection ResultOfMethodCallIgnored
    this.reporterCsvDirectory.mkdirs();
  }

  public String find(String key, String defValue) {
    return options.getOrDefault(key, defValue);
  }

  public int nThreads() {
    return nThreads;
  }

  public Duration reporterPeriod() {
    return reporterPeriod;
  }

  public TimeUnit reporterDurationUnit() {
    return reporterDurationUnit;
  }

  public TimeUnit reporterRateUnit() {
    return reporterRateUnit;
  }

  public File reporterCsvDirectory() {
    return reporterCsvDirectory;
  }

  public String taskName() {
    return testName;
  }

  public MetricRegistry registry() {
    return registry;
  }

  public Duration rampUpDuration() {
    return rampUpDuration;
  }

  public Duration rampUpInterval() {
    return rampUpInterval;
  }

  public long rampUpIterations() {
    return rampUpIterations;
  }

  public Duration taskInterval() {
    return taskInterval;
  }

  public long tasksCount() {
    return tasksCount;
  }

  public Duration testDuration() {
    return testDuration;
  }

  @Override
  public String toString() {
    return "BenchmarksSettings{" +
        "nThreads=" + nThreads +
        ", reporterPeriod=" + reporterPeriod +
        ", reporterDurationUnit=" + reporterDurationUnit +
        ", reporterRateUnit=" + reporterRateUnit +
        ", reporterCsvDirectory=" + reporterCsvDirectory +
        ", testName='" + testName + '\'' +
        ", rampUpDuration=" + rampUpDuration +
        ", rampUpInterval=" + rampUpInterval +
        ", rampUpIterations=" + rampUpIterations +
        ", taskInterval=" + taskInterval +
        ", testDuration=" + testDuration +
        ", tasksCount=" + tasksCount +
        ", options=" + options +
        '}';
  }

  private String minifyClassName(String className) {
    return className.replaceAll("\\B\\w+(\\.[a-zA-Z])", "$1");
  }

  private String allPropertiesAsString() {
    Map<String, String> allProperties = new TreeMap<>(options);
    allProperties.put("nThreads", String.valueOf(nThreads));
    allProperties.put("rampUpDuration", String.valueOf(rampUpDuration));
    allProperties.put("rampUpInterval", String.valueOf(rampUpInterval));
    allProperties.put("rampUpIterations", String.valueOf(rampUpIterations));
    allProperties.put("taskInterval", String.valueOf(taskInterval));
    allProperties.put("tasksCount", String.valueOf(tasksCount));
    allProperties.put("testDuration", String.valueOf(testDuration));
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : allProperties.entrySet()) {
      sb.append("_").append(entry.getKey()).append("=").append(entry.getValue());
    }
    return sb.toString();
  }

  public static class Builder {
    private final Map<String, String> options = new HashMap<>();

    private int nThreads = N_THREADS;
    private Duration reporterPeriod = REPORTER_PERIOD;
    private TimeUnit reporterDurationUnit = REPORTER_DURATION_UNIT;
    private TimeUnit reporterRateUnit = REPORTER_RATE_UNIT;
    private Duration rampUpDuration = RAMP_UP_DURATION;
    private Duration rampUpInterval = RAMP_UP_INTERVAL;
    private long rampUpIterations = RAMP_UP_ITERATIONS;
    private Duration testDuration = TEST_DURATION;
    private long tasksCount = TASKS_COUNT;
    private Duration taskInterval = Duration.ZERO;

    public Builder from(String[] args) {
      this.parse(args);
      return this;
    }

    private Builder() {}

    public Builder nThreads(int numThreads) {
      this.nThreads = numThreads;
      return this;
    }

    public Builder reporterPeriod(Duration reporterPeriod) {
      this.reporterPeriod = reporterPeriod;
      return this;
    }

    public Builder reporterDurationUnit(TimeUnit reporterDurationUnit) {
      this.reporterDurationUnit = reporterDurationUnit;
      return this;
    }

    public Builder reporterRateUnit(TimeUnit reporterRateUnit) {
      this.reporterRateUnit = reporterRateUnit;
      return this;
    }

    public Builder rampUpDuration(Duration rampUpDuration) {
      this.rampUpDuration = rampUpDuration;
      return this;
    }

    public Builder rampUpInterval(Duration rampUpInterval) {
      this.rampUpInterval = rampUpInterval;
      return this;
    }

    public Builder rampUpIterations(long rampUpIterations) {
      this.rampUpIterations = rampUpIterations;
      return this;
    }

    public Builder testDuration(Duration testDuration) {
      this.testDuration = testDuration;
      return this;
    }

    public Builder tasksCount(long taskIterations) {
      this.tasksCount = tasksCount;
      return this;
    }

    public Builder taskInterval(Duration taskInterval) {
      this.taskInterval = taskInterval;
      return this;
    }

    public Builder addOption(String key, String value) {
      this.options.put(key, value);
      return this;
    }

    public BenchmarksSettings build() {
      return new BenchmarksSettings(this);
    }

    private void parse(String[] args) {
      if (args != null) {
        for (String pair : args) {
          String[] keyValue = pair.split("=", 2);
          String key = keyValue[0];
          String value = keyValue[1];
          switch (key) {
            case "nThreads":
              nThreads(Integer.parseInt(value));
              break;
            case "reporterPeriodInSec":
              reporterPeriod(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "rampUpDurationInMillis":
              rampUpDuration(Duration.ofMillis(Long.parseLong(value)));
              break;
            case "rampUpIntervalInMillis":
              rampUpInterval(Duration.ofMillis(Long.parseLong(value)));
              break;
            case "rampUpIterations":
              rampUpIterations(Long.parseLong(value));
              break;
            case "testDurationInSec":
              testDuration(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "tasksCount":
              tasksCount(Long.parseLong(value));
              break;
            case "taskIntervalInMillis":
              taskInterval(Duration.ofMillis(Long.parseLong(value)));
              break;
            default:
              addOption(key, value);
              break;
          }
        }
      }
    }
  }
}
