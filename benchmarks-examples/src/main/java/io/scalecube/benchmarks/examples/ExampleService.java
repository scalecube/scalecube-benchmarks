package io.scalecube.benchmarks.examples;

import reactor.core.publisher.Mono;

public class ExampleService {

  public Mono<String> invoke(String request) {
    return Mono.just(request);
  }
}
