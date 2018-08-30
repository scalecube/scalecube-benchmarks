package io.scalecube.benchmarks.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.scalecube.benchmarks.BenchmarksMetrics;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CodahaleBenchmarksMetrics implements BenchmarksMetrics {

  private final MetricRegistry registry;

  private final Supplier<Boolean> enabled;

  public CodahaleBenchmarksMetrics(MetricRegistry registry,
      Supplier<Boolean> enabled) {
    this.registry = registry;
    this.enabled = enabled;
  }

  @Override
  public BenchmarksTimer timer(String name) {
    Timer timer = registry.timer(name);
    return new BenchmarksTimer() {
      @Override
      public void update(long value, TimeUnit timeUnit) {
        if (enabled.get()) {
          timer.update(value, timeUnit);
        }
      }

      @Override
      public Context time() {
        Timer.Context context = timer.time();
        return () -> {
          if (enabled.get()) {
            context.stop();
          }
        };
      }
    };
  }

  /**
   * Returns meter with specified name.
   *
   * @param name name
   * @return meter with specified name
   */
  @Override
  public BenchmarksMeter meter(String name) {
    Meter meter = registry.meter(name);
    return new BenchmarksMeter() {
      @Override
      public void mark() {
        if (enabled.get()) {
          meter.mark();
        }
      }

      @Override
      public void mark(long value) {
        if (enabled.get()) {
          meter.mark(value);
        }
      }
    };
  }
}
