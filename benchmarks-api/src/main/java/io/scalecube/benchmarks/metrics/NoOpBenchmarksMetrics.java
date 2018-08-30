package io.scalecube.benchmarks.metrics;

import io.scalecube.benchmarks.BenchmarksMetrics;
import java.util.concurrent.TimeUnit;

public class NoOpBenchmarksMetrics implements BenchmarksMetrics {

  @Override
  public BenchmarksTimer timer(String name) {
    return new BenchmarksTimer() {
      @Override
      public void update(long value, TimeUnit timeUnit) {
        // no-op
      }

      @Override
      public Context time() {
        return () -> {
          // no-op
        };
      }
    };
  }

  @Override
  public BenchmarksMeter meter(String name) {
    return new BenchmarksMeter() {
      @Override
      public void mark() {
        // no-op
      }

      @Override
      public void mark(long value) {
        // no-op
      }
    };
  }
}
