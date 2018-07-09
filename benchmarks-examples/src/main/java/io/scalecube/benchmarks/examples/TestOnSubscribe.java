package io.scalecube.benchmarks.examples;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class TestOnSubscribe {
  public static void main(String[] args) {

    // Scenario input (play with these params)
    Callable<String> initializer = () -> "Initialized" + System.currentTimeMillis();
    Consumer<String> tearDown = s -> System.out.println("ShutDown:" + s);
    Consumer<String> underTestCallable = TestOnSubscribe::heavy;

    Duration rampupDuration = ofSeconds(30);
    Duration maxLoadDuration = ofSeconds(60);

    // Calculated (dynamic)
    int users = 100;
    Duration singleScenarioDuration = rampupDuration.plus(maxLoadDuration);
    Duration injectUsersInterval = rampupDuration.dividedBy(users);
    Duration reqInterval = ofMillis(150);
    // int batchOfUsersPerTick = users / usersAtOnce;

    Flux<Object> scenario = Flux
        .using(
            initializer,
            resource -> Mono
                .fromRunnable(() -> underTestCallable.accept(resource))
                .subscribeOn(Schedulers.parallel())
                .delaySubscription(reqInterval)
                .repeat()
                .take(singleScenarioDuration),
            tearDown);

    // Mono<Double> underTest = Mono
    // .defer(() -> Mono.just(heavy()));


    Flux.interval(injectUsersInterval)
        .take(users)
        .flatMap(user -> scenario)
        .blockLast();
  }

  private static double heavy(String ignore) {
    double result = 0.0;
    for (int j = 0; j < 10_000; j++) {
      result = Math.hypot(j, result);
    }
    return result;
  }
}
