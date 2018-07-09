package io.scalecube.benchmarks.examples;

import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.benchmarks.RampUpBenchmarksState;


public class RampUpExampleServiceBenchmarksState extends RampUpBenchmarksState<RampUpExampleServiceBenchmarksState> {

  private ExampleService exampleService;

  public RampUpExampleServiceBenchmarksState(BenchmarksSettings settings) {
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
