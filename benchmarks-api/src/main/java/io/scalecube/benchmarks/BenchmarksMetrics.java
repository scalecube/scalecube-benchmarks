package io.scalecube.benchmarks;

import io.scalecube.benchmarks.metrics.BenchmarksMeter;
import io.scalecube.benchmarks.metrics.BenchmarksTimer;

public interface BenchmarksMetrics {

  BenchmarksTimer timer(String name);

  BenchmarksMeter meter(String name);
}
