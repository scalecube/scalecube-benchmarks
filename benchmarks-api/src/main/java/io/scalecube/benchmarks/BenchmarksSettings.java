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
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class BenchmarksSettings {

  private static final int N_THREADS = Runtime.getRuntime().availableProcessors();
  private static final Duration EXECUTION_TASK_DURATION = Duration.ofSeconds(60);
  private static final Duration EXECUTION_TASK_INTERVAL = Duration.ZERO;
  private static final Duration MINIMAL_INTERVAL = Duration.ofMillis(100);
  private static final Duration REPORTER_INTERVAL = Duration.ofSeconds(3);
  private static final TimeUnit DURATION_UNIT = TimeUnit.MILLISECONDS;

  private static final TimeUnit RATE_UNIT = TimeUnit.SECONDS;
  private static final long NUM_OF_ITERATIONS = Long.MAX_VALUE;
  private static final Duration RAMP_UP_DURATION = Duration.ofSeconds(10);
  private static final boolean CONSOLE_REPORTER_ENABLED = true;
  private static final String ALIAS_PATTERN = "^[.a-zA-Z_0-9]+$";
  private static final Predicate<String> ALIAS_PREDICATE = Pattern.compile(ALIAS_PATTERN).asPredicate();

  private final int nThreads;
  private final Duration executionTaskDuration;
  private final Duration reporterInterval;
  private final File csvReporterDirectory;
  private final String taskName;
  private final TimeUnit durationUnit;
  private final TimeUnit rateUnit;
  private final MetricRegistry registry;
  private final long numOfIterations;
  private final Duration rampUpDuration;
  private final boolean consoleReporterEnabled;
  private final int injectors;
  private final int messageRate;

  private final Duration executionTaskInterval;
  private final Duration rampUpInterval;
  private final int injectorsPerRampUpInterval;
  private final int messagesPerExecutionInterval;

  private final Map<String, String> options;

  public static Builder from(String[] args) {
    Builder builder = new Builder();
    builder.args = args;
    return builder;
  }

  private BenchmarksSettings(Builder builder) {
    this.nThreads = builder.nThreads;
    this.executionTaskDuration = builder.executionTaskDuration;
    this.reporterInterval = builder.reporterInterval;
    this.numOfIterations = builder.numOfIterations;
    this.consoleReporterEnabled = builder.consoleReporterEnabled;
    this.rampUpDuration = builder.rampUpDuration;
    this.options = builder.options;
    this.durationUnit = builder.durationUnit;
    this.rateUnit = builder.rateUnit;
    this.executionTaskInterval = builder.executionTaskInterval;
    this.rampUpInterval = builder.rampUpInterval;
    this.injectorsPerRampUpInterval = builder.injectorsPerRampUpInterval;
    this.messagesPerExecutionInterval = builder.messagesPerExecutionInterval;
    this.injectors = builder.injectors;
    this.messageRate = builder.messageRate;

    this.registry = new MetricRegistry();

    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    this.taskName = minifyClassName(stackTrace[stackTrace.length - 1].getClassName());

    String time = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

    String alias = find("alias", taskName);
    if (!ALIAS_PREDICATE.test(alias)) {
      throw new IllegalArgumentException("alias '" + alias + "' must match pattern " + ALIAS_PATTERN);
    }

    this.csvReporterDirectory = Paths.get("reports", "benchmarks", alias, time).toFile();
    // noinspection ResultOfMethodCallIgnored
    this.csvReporterDirectory.mkdirs();
  }

  public Duration rampUpInterval() {
    return rampUpInterval;
  }

  public int injectorsPerRampUpInterval() {
    return injectorsPerRampUpInterval;
  }

  public int messagesPerExecutionInterval() {
    return messagesPerExecutionInterval;
  }

  public int nThreads() {
    return nThreads;
  }

  public Duration executionTaskDuration() {
    return executionTaskDuration;
  }

  public Duration executionTaskInterval() {
    return executionTaskInterval;
  }

  public Duration reporterInterval() {
    return reporterInterval;
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

  public Duration rampUpDuration() {
    return rampUpDuration;
  }

  public boolean consoleReporterEnabled() {
    return consoleReporterEnabled;
  }

  public int injectors() {
    return injectors;
  }

  public int messageRate() {
    return messageRate;
  }

  private String minifyClassName(String className) {
    return className.replaceAll("\\B\\w+(\\.[a-zA-Z])", "$1");
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("BenchmarksSettings{");
    sb.append("nThreads=").append(nThreads);
    sb.append(", executionTaskDuration=").append(executionTaskDuration);
    sb.append(", reporterInterval=").append(reporterInterval);
    sb.append(", csvReporterDirectory=").append(csvReporterDirectory);
    sb.append(", taskName='").append(taskName).append('\'');
    sb.append(", durationUnit=").append(durationUnit);
    sb.append(", rateUnit=").append(rateUnit);
    sb.append(", registry=").append(registry);
    sb.append(", numOfIterations=").append(numOfIterations);
    sb.append(", rampUpDuration=").append(rampUpDuration);
    sb.append(", consoleReporterEnabled=").append(consoleReporterEnabled);
    sb.append(", injectors=").append(injectors);
    sb.append(", messageRate=").append(messageRate);
    sb.append(", executionTaskInterval=").append(executionTaskInterval);
    sb.append(", rampUpInterval=").append(rampUpInterval);
    sb.append(", injectorsPerRampUpInterval=").append(injectorsPerRampUpInterval);
    sb.append(", messagesPerExecutionInterval=").append(messagesPerExecutionInterval);
    sb.append(", options=").append(options);
    sb.append('}');
    return sb.toString();
  }

  public static class Builder {
    private static final int DEFAULT_INJECTORS = 1000;
    private static final int DEFAULT_MESSAGE_RATE = 1000;
    private final Map<String, String> options;

    private int nThreads = N_THREADS;
    private Duration executionTaskDuration = EXECUTION_TASK_DURATION;
    private Duration executionTaskInterval = EXECUTION_TASK_INTERVAL;
    private Duration reporterInterval = REPORTER_INTERVAL;
    private TimeUnit durationUnit = DURATION_UNIT;
    private TimeUnit rateUnit = RATE_UNIT;
    private long numOfIterations = NUM_OF_ITERATIONS;
    private Duration rampUpDuration = RAMP_UP_DURATION;
    private boolean consoleReporterEnabled = CONSOLE_REPORTER_ENABLED;
    private int injectors;
    private int messageRate;
    private String[] args = new String[] {};
    // dynamic params
    private Duration rampUpInterval;
    private int injectorsPerRampUpInterval;
    private int messagesPerExecutionInterval;

    private Builder() {
      this.options = new HashMap<>();
    }

    private Builder(Builder that) {
      this.options = that.options;
      this.nThreads = that.nThreads;
      this.executionTaskDuration = that.executionTaskDuration;
      this.executionTaskInterval = that.executionTaskInterval;
      this.reporterInterval = that.reporterInterval;
      this.durationUnit = that.durationUnit;
      this.rateUnit = that.rateUnit;
      this.numOfIterations = that.numOfIterations;
      this.rampUpDuration = that.rampUpDuration;
      this.consoleReporterEnabled = that.consoleReporterEnabled;
      this.args = that.args;
      this.rampUpInterval = that.rampUpInterval;
      this.injectorsPerRampUpInterval = that.injectorsPerRampUpInterval;
      this.messagesPerExecutionInterval = that.messagesPerExecutionInterval;
      this.injectors = that.injectors;
      this.messageRate = that.messageRate;
    }

    public Builder nThreads(int numThreads) {
      this.nThreads = numThreads;
      return this;
    }

    public Builder executionTaskDuration(Duration executionTaskDuration) {
      this.executionTaskDuration = executionTaskDuration;
      return this;
    }

    public Builder executionTaskInterval(Duration executionTaskInterval) {
      this.executionTaskInterval = executionTaskInterval;
      return this;
    }

    public Builder reporterInterval(Duration reporterInterval) {
      this.reporterInterval = reporterInterval;
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

    public Builder rampUpDuration(Duration rampUpDuration) {
      this.rampUpDuration = rampUpDuration;
      return this;
    }

    public Builder consoleReporterEnabled(boolean consoleReporterEnabled) {
      this.consoleReporterEnabled = consoleReporterEnabled;
      return this;
    }

    public Builder injectors(int injectors) {
      this.injectors = injectors;
      return this;
    }

    public Builder messageRate(int messageRate) {
      this.messageRate = messageRate;
      return this;
    }

    public BenchmarksSettings build() {
      return new BenchmarksSettings(new Builder(this).parseArgs().calculateDynamicParams());
    }

    private Builder calculateDynamicParams() {
      // if no "injectors specified - don't calculate, means we are
      // running another type of scenario and don't want to overload any parameters
      if (injectors <= 0 && messageRate <= 0) {
        return this;
      }
      if (rampUpDuration.isZero()) {
        throw new IllegalStateException("'rampUpDuration' must be greater than 0");
      }
      if (rampUpDuration.compareTo(executionTaskDuration) > 0) {
        throw new IllegalStateException("'rampUpDuration' must be greater than 'executionTaskDuration'");
      }

      if (injectors <= 0) {
        injectors = DEFAULT_INJECTORS;
      }
      if (messageRate <= 0) {
        messageRate = DEFAULT_MESSAGE_RATE;
      }

      // calculate rampup parameters
      long rampUpDurationMillis = this.rampUpDuration.toMillis();

      if (rampUpDurationMillis / injectors >= MINIMAL_INTERVAL.toMillis()) {
        // 1. Can provide rampup injecting 1 injector per minimal interval
        this.injectorsPerRampUpInterval = 1;
        this.rampUpInterval = Duration.ofMillis(rampUpDurationMillis / injectors);
      } else {
        // 2. Need to inject multiple injectors per minimal interval to provide rampup
        long intervals = Math.floorDiv(rampUpDurationMillis, MINIMAL_INTERVAL.toMillis());
        this.injectorsPerRampUpInterval = (int) Math.floorDiv(injectors, intervals);
        this.rampUpInterval = MINIMAL_INTERVAL;
      }

      // calculate execution parameters
      double injectorRate = (double) messageRate / injectors;
      if (injectorRate <= 1) {
        // 1. Enough injectors to provide the required rate sending each injector 1 msg per (>= 1 second)
        this.messagesPerExecutionInterval = 1;
        this.executionTaskInterval = Duration.ofMillis((long) (1000 / injectorRate));
      } else {
        int maxInjectorsLoad = (int) Math.floorDiv(injectors * 1000, MINIMAL_INTERVAL.toMillis());
        if (maxInjectorsLoad >= messageRate) {
          // 2. Still can provide the required rate sending 1 mesg per tick, execution interval = [MIN_INTERVAL, 1 sec]
          this.messagesPerExecutionInterval = 1;
          this.executionTaskInterval =
              Duration.ofMillis(Math.floorDiv(maxInjectorsLoad, messageRate) * MINIMAL_INTERVAL.toMillis());
        } else {
          // 3. Have to send multiple messages per execution interval , interval already minimum (MIN_INTERVAL)
          this.messagesPerExecutionInterval = Math.floorDiv(messageRate, maxInjectorsLoad);
          this.executionTaskInterval = MINIMAL_INTERVAL;
        }
      }
      return this;
    }

    private Builder parseArgs() {
      if (args != null) {
        for (String pair : args) {
          String[] keyValue = pair.split("=", 2);
          String key = keyValue[0];
          String value = keyValue[1];
          switch (key) {
            case "injectors":
              injectors(Integer.parseInt(value));
              break;
            case "messageRate":
              messageRate(Integer.parseInt(value));
              break;
            case "nThreads":
              nThreads(Integer.parseInt(value));
              break;
            case "executionTaskDurationInSec":
              executionTaskDuration(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "executionTaskIntervalInMillis":
              executionTaskInterval(Duration.ofMillis(Long.parseLong(value)));
              break;
            case "reporterIntervalInSec":
              reporterInterval(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "numOfIterations":
              numOfIterations(Long.parseLong(value));
              break;
            case "rampUpDurationInSec":
              rampUpDuration(Duration.ofSeconds(Long.parseLong(value)));
              break;
            case "consoleReporterEnabled":
              consoleReporterEnabled(Boolean.parseBoolean(value));
              break;
            default:
              addOption(key, value);
              break;
          }
        }
      }
      return this;
    }
  }
}
