package io.scalecube.benchmarks;

import java.time.Duration;

public interface BenchmarkTask {

  BenchmarkSettings settings();

  void schedule(Duration interval);

  void scheduleWithInterval();

  void scheduleNow();
}
