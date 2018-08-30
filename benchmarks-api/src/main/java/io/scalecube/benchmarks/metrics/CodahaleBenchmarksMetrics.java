package io.scalecube.benchmarks.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.scalecube.benchmarks.BenchmarksMetrics;
import java.util.concurrent.TimeUnit;

public class CodahaleBenchmarksMetrics implements BenchmarksMetrics {

  private final MetricRegistry registry;

  public CodahaleBenchmarksMetrics(MetricRegistry registry) {
    this.registry = registry;
  }

  @Override
  public BenchmarksTimer timer(String name) {
    Timer timer = registry.timer(name);
    return new BenchmarksTimer() {
      @Override
      public void update(long value, TimeUnit timeUnit) {
        timer.update(value, timeUnit);
      }

      @Override
      public Context time() {
        Timer.Context context = timer.time();
        return context::stop;
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
        meter.mark();
      }

      @Override
      public void mark(long value) {
        meter.mark(value);
      }
    };
  }
}
