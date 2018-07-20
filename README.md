# scalecube-benchmarks
Microbenchmarks framework

## Download

```xml
<dependency>
  <groupId>io.scalecube</groupId>
  <artifactId>scalecube-benchmarks-api</artifactId>
  <version>1.1.8</version>
</dependency>
```

## Getting started

First, you need to create the settings which will be used while test is running. You can use the builder for it:

```java
public static void main(String[] args) {
  BenchmarksSettings settings = BenchmarksSettings.from(args)
    .nThreads(2)
    .durationUnit(TimeUnit.NANOSECONDS)
    .build();
    ....
}
```

As you see, you could want to override some default settings with the command line, just include them during startup of the test. Or just use the hardcoded style when you write a test scenario.

The second point is you need to prepare some state for your test scenario. You can define how to launch your server, launch some client or just execute some work before running test scenario. For instance, you can create a separated class to reduce writing your scenario in a test or reuse the same state in different scenarios.

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
You can override two methods: `beforeAll` and `afterAll`, these methods will be invoked before test execution and after test termination. The state also may need some settings and it can get them from given BenchmarkSettings via constructor. Also, this state has a few necessary methods, about them below.

### runForSync

```java
void runForSync(Function<SELF, Function<Long, Object>> func)
```

This method intends for execution synchronous tasks. It receives a function, that should return the execution to be tested for the given the state. For instance:

```java
public static void main(String[] args) {
    BenchmarksSettings settings = ...;
    new RouterBenchmarksState(settings).runForSync(state -> {

      Timer timer = state.timer("timer");
      Router router = state.getRouter();

      return iteration -> {
        Timer.Context timeContext = timer.time();
        ServiceReference serviceReference = router.route(request);
        timeContext.stop();
        return serviceReference;
      };
    });
  }
```

As you see, to use this method you need return some function, to generate one you can use the transferred state and iteration's number.

### runForAsync

```java
void runForAsync(Function<SELF, Function<Long, Publisher<?>>> func)
```

This method intends for execution asynchronous tasks. It receives a function, that should return the execution to be tested for the given the state. Note, the unitOfwork should return some `Publisher`. For instance:

```java
  public static void main(String[] args) {
    BenchmarksSettings settings = ...;
    new ExampleServiceBenchmarksState(settings).runForAsync(state -> {

      ExampleService service = state.exampleService();
      Meter meter = state.meter("meter");

      return iteration -> {
        return service.invoke("hello")
            .doOnTerminate(() -> meter.mark());
      };
    });
  }
```

As you see, to use this method you need return some function, to generate one you can use the transferred state and iteration's number.

### runWithRampUp

```java
<T> void runWithRampUp(BiFunction<Long, SELF, Publisher<T>> setUp,
                       Function<SELF, BiFunction<Long, T, Publisher<?>>> func,
                       BiFunction<SELF, T, Mono<Void>> cleanUp)
```

This method intends for execution asynchronous tasks with consumption some resources via ramp-up strategy. It receives three functions, they are necessary to provide all resource life-cycle. The first function is like resource supplier, to implement this one you have access to the active state and a ramp-up iteration's number. And when ramp-up strategy asks for new resources it will be invoked. The second function is like unitOfWork supplier, to implement this one you receive the active state (to take some services or metric's tools), iteration's number and a resource, that was created on the former step. And the last function is like clean-up supplier, that knows how to need release given resource. For instance:

```java
  public static void main(String[] args) {
    BenchmarksSettings settings = BenchmarksSettings.from(args)
        .rampUpDuration(Duration.ofSeconds(10))
        .rampUpInterval(Duration.ofSeconds(1))
        .executionTaskDuration(Duration.ofSeconds(30))
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

It's time to describe the settings in more detail. First, you can see two ramp-up parameters, these are `rampUpDuration` and `rampUpInterval`. They need to specify how long will be processing ramp-up stage and how often will be invoked the resource supplier to receive a new resource. Also, we have two parameters to specify how long will be processing all `unitOfWork`s on the given resource (`executionTaskDuration`) and with another one you can specify some interval that will be applied to invoke the next `unitOfWork` on the given resource (`executionTaskInterval`).

## Additional links
 + [How-to-launch-benchmarks](https://github.com/scalecube/scalecube-benchmarks/wiki/How-to-launch-benchmarks)
