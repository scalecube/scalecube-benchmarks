package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;

import com.codahale.metrics.Timer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RampUpExampleBenchmarksRunner {

  /**
   * Runs example benchmark.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .rampUpDuration(Duration.ofSeconds(10))
        .rampUpInterval(Duration.ofMillis(100))
        .executionTaskDuration(Duration.ofSeconds(30))
        .durationUnit(TimeUnit.NANOSECONDS)
        .build();

    new ExampleServiceBenchmarksState(settings).runForAsync(
        state -> Flux.range(1, 3).map(i -> new ServiceCaller(state.exampleService())),
        state -> {
          Timer timer = state.timer("timer");
          return (iteration, serviceCaller) -> {
            Timer.Context timeContext = timer.time();
            return serviceCaller.call("hello").doOnTerminate(timeContext::stop);
          };
        },
        ServiceCaller::close);
  }

  private static class ServiceCaller {

    private final ExampleService service;

    public ServiceCaller(ExampleService service) {
      this.service = service;
    }

    public Mono<String> call(String request) {
      return service.invoke(request);
    }

    public Mono<Void> close() {
      return Mono.defer(Mono::empty);
    }
  }
}
