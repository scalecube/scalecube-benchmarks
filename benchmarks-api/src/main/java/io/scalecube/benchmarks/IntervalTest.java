package io.scalecube.benchmarks;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class IntervalTest {
  public static void main(String[] args) {
    Flux.merge(
        Flux.interval(Duration.ofSeconds(1))
            .take(Duration.ofSeconds(10))
            .doOnEach(System.out::println)
            .flatMap(connection -> {
              System.out.println("Go connection " + connection);
              return Mono.delay(Duration.ofSeconds(1))
                  .map(aLong1 -> ">>> connected " + connection);
            })
            .map(connection -> {
              System.out.println("Go iteration " + connection);
              return Flux.interval(Duration.ofMillis(100))
                  .map(aLong1 -> "++++ do on iteration, connection: " + connection)
                  .doOnNext(System.out::println)
                  .take(Duration.ofSeconds(1))
                  .doFinally(System.err::println);
            }))
        .blockLast();
  }
}
