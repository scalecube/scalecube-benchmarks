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
  private static final Duration EXECUTION_TASK_TIME = Duration.ofSeconds(60);
  private static final Duration REPORTER_PERIOD = Duration.ofSeconds(3);
  private static final TimeUnit DURATION_UNIT = TimeUnit.MILLISECONDS;
  private static final TimeUnit RATE_UNIT = TimeUnit.SECONDS;
  private static final long NUM_OF_ITERATIONS = Long.MAX_VALUE;

  private final int nThreads;
  private final Duration executionTaskTime;
  private final Duration reporterPeriod;
  private final File csvReporterDirectory;
  private final String taskName;
  private final TimeUnit durationUnit;
  private final TimeUnit rateUnit;
  private final MetricRegistry registry;
  private final long numOfIterations;

  private final Map<String, String> options;

  public static Builder from(String[] args) {
    return new Builder().from(args);
  }

  private BenchmarksSettings(Builder builder) {
    this.nThreads = builder.nThreads;
    this.executionTaskTime = builder.executionTaskTime;
    this.reporterPeriod = builder.reporterPeriod;
    this.numOfIterations = builder.numOfIterations;

    this.options = builder.options;

    this.registry = new MetricRegistry();

    this.durationUnit = builder.durationUnit;
    this.rateUnit = builder.rateUnit;

    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    this.taskName = minifyClassName(stackTrace[stackTrace.length - 1].getClassName());

    String time = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    this.csvReporterDirectory = Paths.get("benchmarks", "results", taskName + allPropertiesAsString(), time).toFile();
    // noinspection ResultOfMethodCallIgnored
    this.csvReporterDirectory.mkdirs();
  }

  public int nThreads() {
    return nThreads;
  }

  public Duration executionTaskTime() {
    return executionTaskTime;
  }

  public Duration reporterPeriod() {
    return reporterPeriod;
  }

  public File csvReporterDirectory() {
    return csvReporterDirectory;
  }

  public String taskName() {
    return taskName;
  }

  public String find(String key, String defValue) {
    return options.getOrDefault(key, defValue);
  }

  public MetricRegistry registry() {
    return registry;
  }

  public TimeUnit durationUnit() {
    return durationUnit;
  }

  public TimeUnit rateUnit() {
    return rateUnit;
  }

  public long numOfIterations() {
    return numOfIterations;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("BenchmarksSettings{");
    sb.append("nThreads=").append(nThreads);
    sb.append(", executionTaskTime=").append(executionTaskTime);
    sb.append(", numOfIterations=").append(numOfIterations);
    sb.append(", reporterPeriod=").append(reporterPeriod);
    sb.append(", csvReporterDirectory=").append(csvReporterDirectory);
    sb.append(", taskName='").append(taskName).append('\'');
    sb.append(", durationUnit=").append(durationUnit);
    sb.append(", rateUnit=").append(rateUnit);
    sb.append(", registry=").append(registry);
    sb.append(", options=").append(options);
    sb.append('}');
    return sb.toString();
  }

  private String minifyClassName(String className) {
    return className.replaceAll("\\B\\w+(\\.[a-zA-Z])","$1");
  }

  private String allPropertiesAsString() {
    Map<String, String> allProperties = new TreeMap<>(options);
    allProperties.put("nThreads", String.valueOf(nThreads));
    allProperties.put("executionTaskTime", String.valueOf(executionTaskTime));
    allProperties.put("numOfIterations", String.valueOf(numOfIterations));
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : allProperties.entrySet()) {
      sb.append("_").append(entry.getKey()).append("=").append(entry.getValue());
    }
    return sb.toString();
  }

  public static class Builder {
    private final Map<String, String> options = new HashMap<>();

    private int nThreads = N_THREADS;
    private Duration executionTaskTime = EXECUTION_TASK_TIME;
    private Duration reporterPeriod = REPORTER_PERIOD;
    private TimeUnit durationUnit = DURATION_UNIT;
    private TimeUnit rateUnit = RATE_UNIT;
    private long numOfIterations = NUM_OF_ITERATIONS;

    public Builder from(String[] args) {
      this.parse(args);
      return this;
    }

    private Builder() {}

    public Builder nThreads(int numThreads) {
      this.nThreads = numThreads;
      return this;
    }

    public Builder executionTaskTime(Duration executionTaskTime) {
      this.executionTaskTime = executionTaskTime;
      return this;
    }

    public Builder reporterPeriod(Duration reporterPeriod) {
      this.reporterPeriod = reporterPeriod;
      return this;
    }

    public Builder addOption(String key, String value) {
      this.options.put(key, value);
      return this;
    }

    public Builder durationUnit(TimeUnit durationUnit) {
      this.durationUnit = durationUnit;
      return this;
    }

    public Builder rateUnit(TimeUnit rateUnit) {
      this.rateUnit = rateUnit;
      return this;
    }

    public Builder numOfIterations(long numOfIterations) {
      this.numOfIterations = numOfIterations;
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
            case "executionTaskTimeInSec":
              executionTaskTime(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "reporterPeriodInSec":
              reporterPeriod(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "numOfIterations":
              numOfIterations(Long.parseLong(value));
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
