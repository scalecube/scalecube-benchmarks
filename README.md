# scalecube-benchmarks
Benchmarks framework

## Download

```xml
<dependency>
  <groupId>io.scalecube</groupId>
  <artifactId>scalecube-benchmarks-api</artifactId>
  <version>1.1.9</version>
</dependency>
```

## Getting started

First, you need to create the settings which will be used while test is running:

```java
public static void main(String[] args) {
  BenchmarksSettings settings = BenchmarksSettings.from(args)
    .nThreads(2)
    .durationUnit(TimeUnit.NANOSECONDS)
    .build();
    ....
}
```

To override default settings with the command line include them as program variables at startup of the test. Or set them up programmatically inside a runner. See complete list of settings [here](https://github.com/scalecube/scalecube-benchmarks/wiki/Benchmarks-settings).

The second point is you need to prepare BenchmarksState instance for test scenario. You can define how to launch your server, launch some client or just execute some work before running test scenario. For instance, you can create a separated class to reuse the same BenchmarksState class in different scenarios:

```java
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
```
You can override two methods: `beforeAll` and `afterAll`, these methods will be invoked before test execution and after test termination. The state class has a few necessary methods, about them below.

### runForSync

```java
void runForSync(Function<SELF, Function<Long, Object>> func)
```

This method intends for execution of synchronous tasks. As input you have to provide a function that should return the execution to be tested for the given the state at certain iteration. For instance:

```java
public static void main(String[] args) {
    BenchmarksSettings settings = ...;
    new RouterBenchmarksState(settings).runForSync(state -> {

      // prepare metrics, tools and etc. before actual test iterations start
      Timer timer = state.timer("timer");
      Router router = state.getRouter();

      // call and measure test scenario
      return iteration -> {
        Timer.Context timeContext = timer.time();
        // NOTE: this call is synchronous
        ServiceReference serviceReference = router.route(request);
        timeContext.stop();
        return serviceReference;
      };
    });
  }
```

### runForAsync

```java
void runForAsync(Function<SELF, Function<Long, Publisher<?>>> func)
```

This method intends for execution of asynchronous tasks. It receives a function, that should return the execution to be tested for the given the state. Note, the unitOfwork (see `Function<Long, Publisher<?>>` from the signature) should return some `Publisher`. For instance:

```java
  public static void main(String[] args) {
    BenchmarksSettings settings = ...;
    new ExampleServiceBenchmarksState(settings).runForAsync(state -> {

      // prepare metrics before test interations start
      ExampleService service = state.exampleService();
      Meter meter = state.meter("meter");

      return iteration -> {
        // NOTE: this is asynchronous call, method invoke retuns a publisher
        return service.invoke("hello")
            .doOnTerminate(() -> meter.mark());
      };
    });
  }
```

### runWithRampUp

```java
<T> void runWithRampUp(BiFunction<Long, SELF, Publisher<T>> setUp,
                       Function<SELF, BiFunction<Long, T, Publisher<?>>> func,
                       BiFunction<SELF, T, Mono<Void>> cleanUp)
```

This method intends for execution of asynchronous tasks with consumption some resources via ramp-up strategy. It receives three functions: setUp, unitOfWork, cleanUp. 
First function `setUp` is a resource supplier; this function is being called on every ramp-up iteration.
The second function (see `BiFunction<Long, T, Publisher<?>>`) is like unitOfWork supplier, actual test scenario must define here; this unitOfWork is being called on every test iteration and accepts as input a product of corresponding `setUp` call.
The third one is `cleanUp` supplier, that knows how to release resources that was created at ramp-up stage. More about ramp-up algorithm [here](https://github.com/scalecube/scalecube-benchmarks/wiki/Ramp-up-test-scenarios).

For instance:

```java
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .rampUpDuration(Duration.ofSeconds(10))
        .rampUpInterval(Duration.ofSeconds(1))
        .scenarioDuration(Duration.ofSeconds(30))
        .executionTaskInterval(Duration.ofMillis(100))
        .build();

    new ExampleServiceBenchmarksState(settings).runWithRampUp(
        (rampUpIteration, state) -> Mono.just(new ServiceCaller(state.exampleService()),
        state -> {
          Meter meter = state.meter("meter");
          return (iteration, serviceCaller) -> serviceCaller.call("hello").doOnTerminate(meter::mark);
        },
        (state, serviceCaller) -> serviceCaller.close());
  }
```
