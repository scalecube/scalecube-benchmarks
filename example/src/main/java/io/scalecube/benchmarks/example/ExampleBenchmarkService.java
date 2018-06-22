package io.scalecube.benchmarks.example;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;

import reactor.core.publisher.Mono;

@Service
public interface ExampleBenchmarkService {

  @ServiceMethod
  Mono<String> requestOne(String request);
}
