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
        .rampUpDuration(Duration.ofSeconds(10))
        .rampUpInterval(Duration.ofSeconds(1))
        .testDuration(Duration.ofSeconds(60))
        .taskInterval(Duration.ofMillis(10))
        .reporterDurationUnit(TimeUnit.NANOSECONDS)
        .build();

    new ExampleServiceBenchmarksState(settings).runForAsync(
        state -> Mono.defer(() -> Mono.just(new ServiceCall(state.exampleService()))),
        state -> {
          Timer timer = state.timer("timer");

          return (iteration, serviceCall) -> {
            Timer.Context timeContext = timer.time();
            return serviceCall.call("hello")
                .doOnTerminate(timeContext::stop);
          };
        },
        ServiceCall::close);
  }

  private static class ServiceCall {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id = COUNTER.incrementAndGet();
    private final ExampleService service;

    public ServiceCall(ExampleService service) {
      this.service = service;
    }

    public Mono<String> call(String request) {
      return Mono.defer(() -> service.invoke("#" + id + "--> " + request));
    }

    public Mono<Void> close() {
      return Mono.defer(() -> {
        COUNTER.set(0);
        return Mono.empty();
      });
    }
  }
}
