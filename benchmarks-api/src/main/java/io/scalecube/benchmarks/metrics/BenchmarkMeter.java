package io.scalecube.benchmarks.metrics;

public interface BenchmarkMeter {

  void mark();

  void mark(long value);
}
