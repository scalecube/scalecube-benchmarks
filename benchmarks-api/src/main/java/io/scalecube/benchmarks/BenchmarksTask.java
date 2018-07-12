package io.scalecube.benchmarks;

import static io.scalecube.benchmarks.BenchmarksTask.Status.COMPLETED;
import static io.scalecube.benchmarks.BenchmarksTask.Status.COMPLETING;
import static io.scalecube.benchmarks.BenchmarksTask.Status.SCHEDULED;
import static io.scalecube.benchmarks.BenchmarksTask.Status.STARTED;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BenchmarksTask<SELF extends BenchmarksState<SELF>, T> implements Runnable {

  public enum Status {
    STARTED, SCHEDULED, COMPLETING, COMPLETED
  }

  private final SELF benchmarksState;
  private final Function<SELF, Mono<T>> supplier;
  private final BiFunction<Long, T, Publisher<?>> unitOfWork;
  private final Function<T, Mono<Void>> cleanUp;
  private final long numOfIterations;
  private final Duration executionTaskTime;
  private final Scheduler scheduler;

  private final AtomicLong iterationsCounter = new AtomicLong();
  private final AtomicReference<Status> taskStatus = new AtomicReference<>();
  private final AtomicReference<T> supplierResultReference = new AtomicReference<>();
  private final CompletableFuture<Void> taskCompletionFuture = new CompletableFuture<>();

  public BenchmarksTask(SELF benchmarksState,
      Function<SELF, Mono<T>> supplier,
      BiFunction<Long, T, Publisher<?>> unitOfWork,
      Function<T, Mono<Void>> cleanUp,
      Scheduler scheduler) {

    this.benchmarksState = benchmarksState;
    this.supplier = supplier;
    this.unitOfWork = unitOfWork;
    this.cleanUp = cleanUp;
    this.scheduler = scheduler;
    this.numOfIterations = benchmarksState.settings.numOfIterations();
    this.executionTaskTime = benchmarksState.settings.executionTaskTime();
  }

  @Override
  public void run() {
    if (isCompleted()) {
      return; // this is the end
    }

    if (iterationsCounter.get() >= numOfIterations) {
      startCompleting();
      return; // this is the end
    }

    if (isScheduled()) { // executing
      long iter = iterationsCounter.incrementAndGet();
      T res = supplierResultReference.get(); // not null here
      Flux.from(unitOfWork.apply(iter, res))
          .doOnError(ignore -> {
            // no-op
          })
          .subscribe();
      scheduler.schedule(this);
      return;
    }

    if (setStarted()) { // started
      scheduler.schedule(this::startCompleting, executionTaskTime.toMillis(), TimeUnit.MILLISECONDS);

      try {
        Mono<T> supplierMono = supplier.apply(benchmarksState);
        if (supplierMono == null) {
          startCompleting();
          return;
        }
        supplierMono
            .doOnError(this::startCompletingWithError)
            .subscribe(res -> {
              if (res == null) { // wtf
                startCompleting();
              } else if (setScheduled()) { // scheduled
                supplierResultReference.set(res);
                scheduler.schedule(this);
              }
            });
      } catch (Throwable ex) {
        startCompletingWithError(ex);
      }
    }
  }

  private boolean setCompleted() {
    boolean compareAndSet = taskStatus.compareAndSet(COMPLETING, COMPLETED);
    taskCompletionFuture.obtrudeValue(null);
    return compareAndSet;
  }

  private boolean setCompletedWithError(Throwable throwable) {
    boolean compareAndSet = taskStatus.compareAndSet(COMPLETING, COMPLETED);
    taskCompletionFuture.obtrudeException(throwable);
    return compareAndSet;
  }

  private boolean setStarted() {
    return taskStatus.compareAndSet(null, STARTED);
  }

  private boolean setScheduled() {
    return taskStatus.compareAndSet(STARTED, SCHEDULED);
  }

  private boolean trySetCompleting() {
    return taskStatus.compareAndSet(null, COMPLETING)
        || taskStatus.compareAndSet(STARTED, COMPLETING)
        || taskStatus.compareAndSet(SCHEDULED, COMPLETING);
  }

  private boolean isCompleted() {
    return taskStatus.get() == COMPLETED;
  }

  private boolean isScheduled() {
    return taskStatus.get() == SCHEDULED;
  }

  private void startCompletingWithError(Throwable throwable) {
    if (trySetCompleting()) {
      T res = supplierResultReference.get();
      if (res == null) {
        setCompletedWithError(throwable);
        return;
      }
      try {
        Mono<Void> voidMono = cleanUp.apply(res);
        if (voidMono == null) {
          setCompletedWithError(throwable);
          return;
        }
        voidMono.subscribe(
            aVoid -> setCompletedWithError(throwable),
            ex -> setCompletedWithError(throwable),
            () -> setCompletedWithError(throwable));
      } catch (Throwable ex) {
        setCompletedWithError(throwable);
      }
    }
  }

  private void startCompleting() {
    if (trySetCompleting()) {
      T res = supplierResultReference.get();
      if (res == null) {
        setCompleted();
        return;
      }
      try {
        Mono<Void> voidMono = cleanUp.apply(res);
        if (voidMono == null) {
          setCompleted();
          return;
        }
        voidMono.subscribe(
            aVoid -> setCompleted(),
            this::setCompletedWithError,
            this::setCompleted);
      } catch (Throwable ex) {
        setCompletedWithError(ex);
      }
    }
  }
}
