package io.scalecube.benchmarks;

import reactor.core.publisher.Flux;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.LongStream;


public class RampUpBenchmarksState<SELF extends RampUpBenchmarksState<SELF>> extends BenchmarksState<SELF> {

  public RampUpBenchmarksState(BenchmarksSettings settings) {
    super(settings);
  }

  public final <T> void run(Function<SELF, T> supplier, Function<SELF, BiFunction<Long, T, Publisher<?>>> func) {
    // noinspection unchecked
    @SuppressWarnings("unchecked")
    SELF self = (SELF) this;
    try {
      self.start();

      BiFunction<Long, T, Publisher<?>> unitOfWork = func.apply(self);

      long unitOfWorkNumber = settings.numOfIterations();
      Duration unitOfWorkDuration = settings.executionTaskTime();


      Duration rampUpDuration = Duration.ofSeconds(1);
      long rampUpCount = 10;

      int prefetch = Integer.MAX_VALUE;
      int concurrency = Integer.MAX_VALUE;

      Flux.interval(rampUpDuration)
          .take(rampUpCount)
          .map(i -> supplier.apply(self))
          .flatMap(supplier0 -> Flux.merge(Flux.fromStream(LongStream.range(0, unitOfWorkNumber).boxed())
              .publishOn(scheduler, prefetch).map(i -> unitOfWork.apply(i, supplier0)), concurrency, prefetch)
              .take(unitOfWorkDuration))
          .blockLast();
    } finally {
      self.shutdown();
    }
  }

}
