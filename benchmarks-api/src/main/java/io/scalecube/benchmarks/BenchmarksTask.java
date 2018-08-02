package io.scalecube.benchmarks;

import static io.scalecube.benchmarks.BenchmarksTask.Status.COMPLETED;
import static io.scalecube.benchmarks.BenchmarksTask.Status.COMPLETING;
import static io.scalecube.benchmarks.BenchmarksTask.Status.SCHEDULED;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public class BenchmarksTask<SELF extends BenchmarksState<SELF>, T> implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksTask.class);

  public enum Status {
    SCHEDULED, COMPLETING, COMPLETED
  }

  private final SELF benchmarksState;
  private final T setUpResult;
  private final BiFunction<Long, T, Publisher<?>> unitOfWork;
  private final BiFunction<SELF, T, Mono<Void>> cleanUp;
  private final long numOfIterations;
  private final Duration executionTaskDuration;
  private final Duration executionTaskInterval;
  private final Scheduler scheduler;

  private final AtomicLong iterationsCounter = new AtomicLong();
  private final AtomicReference<Status> taskStatus = new AtomicReference<>();
  private final CompletableFuture<Void> taskCompletionFuture = new CompletableFuture<>();
  private final AtomicReference<Disposable> scheduledCompletingTask = new AtomicReference<>();

  /**
   * Constructs benchmark task.
   * 
   * @param setUpResult a result of setUp function.
   * @param unitOfWork an unit of work.
   * @param cleanUp a function that should clean up some T's resources.
   * @param scheduler a scheduler.
   */
  public BenchmarksTask(SELF benchmarksState,
      T setUpResult,
      BiFunction<Long, T, Publisher<?>> unitOfWork,
      BiFunction<SELF, T, Mono<Void>> cleanUp,
      Scheduler scheduler) {

    this.benchmarksState = benchmarksState;
    this.setUpResult = setUpResult;
    this.unitOfWork = unitOfWork;
    this.cleanUp = cleanUp;
    this.scheduler = scheduler;
    this.numOfIterations = benchmarksState.settings.numOfIterations();
    this.executionTaskDuration = benchmarksState.settings.executionTaskDuration();
    this.executionTaskInterval = benchmarksState.settings.executionTaskInterval();
  }

  @Override
  public void run() {
    if (isCompleted()) {
      return; // this is the end
    }

    if (iterationsCounter.get() >= numOfIterations) {
      LOGGER.debug("Task is completed due to iterations limit: " + numOfIterations);
      startCompleting();
      return; // this is the end
    }

    if (isScheduled()) { // executing
      long iter = iterationsCounter.incrementAndGet();

      Flux
          .range(0, benchmarksState.settings.messagesPerExecutionInterval())
          .flatMap($ -> Flux.from(unitOfWork.apply(iter, setUpResult)))
          .doOnError(ex -> LOGGER.warn("Exception occured on unitOfWork at iteration: {}, cause: {}", iter, ex))
          .subscribe();
      if (executionTaskInterval.isZero()) {
        scheduler.schedule(this);
      } else {
        scheduler.schedule(this, executionTaskInterval.toMillis(), TimeUnit.MILLISECONDS);
      }
      return;
    }

    if (setScheduled()) { // scheduled
      scheduledCompletingTask
          .set(scheduler.schedule(() -> {
            LOGGER.debug("Task is completing due to execution duration limit: " + executionTaskDuration.toMillis());
            startCompleting();
          }, executionTaskDuration.toMillis(), TimeUnit.MILLISECONDS));

      LOGGER.debug("Obtained setUp result: {}, now scheduling", setUpResult);
      scheduler.schedule(this);
    }
  }

  public Mono<Void> completionMono() {
    return Mono.fromFuture(taskCompletionFuture);
  }

  private boolean setCompleted() {
    final boolean compareAndSet = taskStatus.compareAndSet(COMPLETING, COMPLETED);
    Disposable disposable = scheduledCompletingTask.get();
    if (disposable != null) {
      disposable.dispose();
    }
    LOGGER.debug("Task is completed");
    taskCompletionFuture.obtrudeValue(null);
    return compareAndSet;
  }

  private boolean setCompletedWithError(Throwable throwable) {
    final boolean compareAndSet = taskStatus.compareAndSet(COMPLETING, COMPLETED);
    Disposable disposable = scheduledCompletingTask.get();
    if (disposable != null) {
      disposable.dispose();
    }
    LOGGER.error("Task is completed with error: {}", throwable);
    taskCompletionFuture.obtrudeException(throwable);
    return compareAndSet;
  }

  private boolean setScheduled() {
    return taskStatus.compareAndSet(null, SCHEDULED);
  }

  private boolean trySetCompleting() {
    return taskStatus.compareAndSet(null, COMPLETING) || taskStatus.compareAndSet(SCHEDULED, COMPLETING);
  }

  private boolean isCompleted() {
    return taskStatus.get() == COMPLETED;
  }

  private boolean isScheduled() {
    return taskStatus.get() == SCHEDULED;
  }

  private void startCompleting() {
    if (trySetCompleting()) {
      try {
        Mono<Void> voidMono = cleanUp.apply(benchmarksState, setUpResult);
        voidMono.subscribe(
            aVoid -> setCompleted(),
            ex -> {
              LOGGER.error("Exception occured on cleanUp, cause: {}", ex);
              setCompletedWithError(ex);
            },
            this::setCompleted);
      } catch (Throwable ex) {
        LOGGER.error("Exception occured on cleanUp, cause: {}", ex);
        setCompletedWithError(ex);
      }
    }
  }
}
