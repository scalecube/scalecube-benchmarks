package io.scalecube.benchmarks;

import reactor.core.scheduler.Scheduler;

public interface BenchmarkTask extends Runnable {

  BenchmarkSettings settings();

  Scheduler scheduler();
}
