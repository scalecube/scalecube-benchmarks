package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.benchmarks.BenchmarksState;

public class ExampleServiceBenchmarksState extends BenchmarksState<ExampleServiceBenchmarksState> {

  private ExampleService exampleService;

  public ExampleServiceBenchmarksState(BenchmarksSettings settings) {
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
