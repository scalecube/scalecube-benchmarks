package io.scalecube.benchmarks.metrics;

public interface BenchmarksMeter {

  void mark();

  void mark(long value);
}
