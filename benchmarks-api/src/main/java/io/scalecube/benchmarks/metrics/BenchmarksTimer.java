package io.scalecube.benchmarks.metrics;

import java.util.concurrent.TimeUnit;

public interface BenchmarksTimer {

  void update(long value, TimeUnit timeUnit);

  Context time();

  interface Context {

    void stop();
  }
}
