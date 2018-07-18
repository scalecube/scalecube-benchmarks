package io.scalecube.benchmarks.examples;

import reactor.core.publisher.Mono;

public class ExampleService {

  public Mono<String> invoke(String request) {
    return Mono.defer(() -> Mono.just(syncInvoke(request)));
  }

  public String syncInvoke(String request) {
    return request + hardTask();
  }

  private Double hardTask() {
    return Math.hypot(100, 100);
  }
}
