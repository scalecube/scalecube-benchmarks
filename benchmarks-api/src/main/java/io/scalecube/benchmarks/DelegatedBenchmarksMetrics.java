package io.scalecube.benchmarks;

import io.scalecube.benchmarks.metrics.BenchmarksMeter;
import io.scalecube.benchmarks.metrics.BenchmarksTimer;
import java.util.function.Supplier;

public class DelegatedBenchmarksMetrics implements BenchmarksMetrics {

  private final Supplier<BenchmarksMetrics> benchmarksMetricsSupplier;

  public DelegatedBenchmarksMetrics(Supplier<BenchmarksMetrics> benchmarksMetricsSupplier) {
    this.benchmarksMetricsSupplier = benchmarksMetricsSupplier;
  }

  @Override
  public BenchmarksTimer timer(String name) {
    return benchmarksMetricsSupplier.get().timer(name);
  }

  @Override
  public BenchmarksMeter meter(String name) {
    return benchmarksMetricsSupplier.get().meter(name);
  }
}
