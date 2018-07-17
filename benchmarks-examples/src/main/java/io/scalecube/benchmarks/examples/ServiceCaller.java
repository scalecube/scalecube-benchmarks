package io.scalecube.benchmarks.examples;

import reactor.core.publisher.Mono;

public class ServiceCaller {

  private final ExampleService service;

  public ServiceCaller(ExampleService service) {
    this.service = service;
  }

  public Mono<String> call(String request) {
    return service.invoke(request);
  }

  public Mono<String> failingCall(String request) {
    return Mono.error(new RuntimeException(request));
  }

  public Mono<Void> close() {
    return Mono.defer(Mono::empty);
  }
}
