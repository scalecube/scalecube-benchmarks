package io.scalecube.benchmarks;

import io.scalecube.benchmarks.metrics.BenchmarkMeter;
import io.scalecube.benchmarks.metrics.BenchmarkTimer;

public interface BenchmarkMetrics {

  BenchmarkTimer timer(String name);

  BenchmarkMeter meter(String name);
}
