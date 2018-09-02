package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.benchmarks.BenchmarkState;

public class ExampleServiceBenchmarkState extends BenchmarkState<ExampleServiceBenchmarkState> {

  private ExampleService exampleService;

  public ExampleServiceBenchmarkState(BenchmarkSettings settings) {
    super(settings);
  }

  @Override
  public void beforeAll() {
    this.exampleService = new ExampleService();
  }

  public ExampleService exampleService() {
    return exampleService;
  }
}
