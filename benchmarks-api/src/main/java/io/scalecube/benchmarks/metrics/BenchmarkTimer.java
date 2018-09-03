package io.scalecube.benchmarks.metrics;

import java.util.concurrent.TimeUnit;

public interface BenchmarkTimer {

  Context NO_OP_CONTEXT =
      () -> {
        // no-op
      };

  void update(long value, TimeUnit timeUnit);

  Context time();

  interface Context {

    void stop();
  }
}
