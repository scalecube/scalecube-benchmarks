package io.scalecube.benchmarks.example;

import reactor.core.publisher.Mono;

public class BenchmarkServiceImpl implements BenchmarkService {

  @Override
  public Mono<String> requestOne(String request) {
    return Mono.just(request);
  }

}
