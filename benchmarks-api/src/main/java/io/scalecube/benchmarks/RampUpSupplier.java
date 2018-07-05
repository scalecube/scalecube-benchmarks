package io.scalecube.benchmarks;

import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class RampUpSupplier<T> {

  private Queue<> queue = new DelayQueue<>();

  private static class Job<T> implements Delayed {

    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed o) {
      return 0;
    }
  }
}
