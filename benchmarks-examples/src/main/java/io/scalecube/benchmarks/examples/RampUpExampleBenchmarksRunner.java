package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RampUpExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .numOfIterations(10000000)
        .executionTaskTime(Duration.ofSeconds(5))
        .rampUpAllDuration(Duration.ofSeconds(10))
        .rampUpDurationPerSupplier(Duration.ofSeconds(1))
        .durationUnit(TimeUnit.NANOSECONDS)
        .build();

    new ExampleServiceBenchmarksState(settings).runForAsync(state -> Mono.just(new ServiceCall()),
        state -> {
          ExampleService service = state.exampleService();
          Timer timer = state.timer("timer");

          return (iteration, serviceCall) -> {
            Timer.Context timeContext = timer.time();
            return serviceCall.call(service, "hello")
                .doOnTerminate(timeContext::stop);
          };
        }, ServiceCall::close);
  }

  private static class ServiceCall {

    private final static AtomicInteger COUNTER = new AtomicInteger();

    private final int id = COUNTER.incrementAndGet();

    public Mono<String> call(ExampleService service, String request) {
      return Mono.defer(() -> service.invoke("#" + id + "--> " + request));
    }

    public Mono<Void> close() {
      return Mono.defer(() -> {
        System.err.println("close");
        COUNTER.set(0);
        return Mono.empty();
      });
    }
  }
}
