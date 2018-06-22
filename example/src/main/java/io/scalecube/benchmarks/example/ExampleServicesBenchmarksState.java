package io.scalecube.benchmarks.example;

import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.benchmarks.BenchmarksState;
import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceCall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class ExampleServicesBenchmarksState extends BenchmarksState<ExampleServicesBenchmarksState> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExampleServicesBenchmarksState.class);

  private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(6);

  private final Object[] services;

  private Microservices seed;
  private Microservices node;

  public ExampleServicesBenchmarksState(BenchmarksSettings settings, Object... services) {
    super(settings);
    this.services = services;
  }

  @Override
  public void beforeAll() {
    seed = Microservices.builder()
        .metrics(settings.registry())
        .startAwait();

    node = Microservices.builder()
        .metrics(settings.registry())
        .seeds(seed.cluster().address())
        .services(services)
        .startAwait();

    LOGGER.info("Seed address: {}, services address: {}, seed serviceRegistry: {}",
        seed.cluster().address(), node.serviceAddress(), seed.serviceRegistry().listServiceReferences());
  }

  @Override
  public void afterAll() {
    if (node != null) {
      try {
        node.shutdown().block(SHUTDOWN_TIMEOUT);
      } catch (Throwable ignore) {
        // NOP
      }
    }

    if (seed != null) {
      try {
        seed.shutdown().block(SHUTDOWN_TIMEOUT);
      } catch (Throwable ignore) {
        // NOP
      }
    }
  }

  public Microservices seed() {
    return seed;
  }

  public <T> T service(Class<T> serviceClass) {
    return seed.call().create().api(serviceClass);
  }

  public ServiceCall serviceCall() {
    return seed.call().create();
  }
}
