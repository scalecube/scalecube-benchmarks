package io.scalecube.benchmarks;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.LongStream;


public class RampUpBenchmarksState<SELF extends RampUpBenchmarksState<SELF>> {

  public final <T> void run(Supplier<T> supplier, Function<SELF, BiFunction<Long, T, Publisher<?>>> func) {
    // noinspection unchecked
    @SuppressWarnings("unchecked")
    SELF self = (SELF) this;
    try {
//      self.start();

      BiFunction<Long, T, Publisher<?>> unitOfWork = func.apply(self);

      long numOfIterations = Long.MAX_VALUE;
      Scheduler scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(4));
      Duration executionTaskTime = Duration.ofMinutes(5);

      int prefetch = Integer.MAX_VALUE;
      int concurrency = Integer.MAX_VALUE;
      Flux<Long> fromStream = Flux.fromStream(LongStream.range(0, numOfIterations).boxed());

      Flux.merge(fromStream.publishOn(scheduler, prefetch).map(l -> ), concurrency, prefetch)
          .take(executionTaskTime)
          .blockLast();
    } finally {
//      self.shutdown();
    }
  }

}
