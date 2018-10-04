package io.scalecube.benchmarks.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.scalecube.benchmarks.BenchmarkMetrics;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CodahaleBenchmarkMetrics implements BenchmarkMetrics {

  private final MetricRegistry registry;
  private final Supplier<Boolean> enabled;

  public CodahaleBenchmarkMetrics(MetricRegistry registry, Supplier<Boolean> enabled) {
    this.registry = registry;
    this.enabled = enabled;
  }

  @Override
  public BenchmarkTimer timer(String name) {
    Timer timer = registry.timer(name);
    return new BenchmarkTimer() {
      @Override
      public void update(long value, TimeUnit timeUnit) {
        if (enabled.get()) {
          timer.update(value, timeUnit);
        }
      }

      @Override
      public Context time() {
        if (enabled.get()) {
          Timer.Context context = timer.time();
          return context::stop;
        } else {
          return NO_OP_CONTEXT;
        }
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
  public BenchmarkMeter meter(String name) {
    Meter meter = registry.meter(name);
    return new BenchmarkMeter() {
      @Override
      public void mark() {
        meter.mark();
      }

      @Override
      public void mark(long value) {
        meter.mark(value);
      }
    };
  }
}
