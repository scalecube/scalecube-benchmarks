package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.benchmarks.BenchmarksState;


public class ExampleServicesBenchmarksState extends BenchmarksState<ExampleServicesBenchmarksState> {

  private ExampleService exampleService;

  public ExampleServicesBenchmarksState(BenchmarksSettings settings) {
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
