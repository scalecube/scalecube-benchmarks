package io.scalecube.benchmarks;

import static io.scalecube.benchmarks.BenchmarksTask.Status.COMPLETED;
import static io.scalecube.benchmarks.BenchmarksTask.Status.COMPLETING;
import static io.scalecube.benchmarks.BenchmarksTask.Status.SCHEDULED;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class BenchmarksTask<S extends BenchmarksState<S>, T> implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksTask.class);

  public enum Status {
    SCHEDULED,
    COMPLETING,
    COMPLETED
  }

  private final S benchmarksState;
  private final T setUpResult;
  private final BiFunction<Long, T, Publisher<?>> unitOfWork;
  private final BiFunction<S, T, Mono<Void>> cleanUp;
  private final long numOfIterations;
  private final Duration scenarioDuration;
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
  public BenchmarksTask(
      S benchmarksState,
      T setUpResult,
      BiFunction<Long, T, Publisher<?>> unitOfWork,
      BiFunction<S, T, Mono<Void>> cleanUp,
      Scheduler scheduler) {

    this.benchmarksState = benchmarksState;
    this.setUpResult = setUpResult;
    this.unitOfWork = unitOfWork;
    this.cleanUp = cleanUp;
    this.scheduler = scheduler;
    this.numOfIterations = benchmarksState.settings.numOfIterations();
    this.scenarioDuration = benchmarksState.settings.scenarioDuration();
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

      Flux.from(unitOfWork.apply(iter, setUpResult))
          .doOnError(
              ex ->
                  LOGGER.warn(
                      "Exception occurred on unitOfWork at iteration: {}, cause: {}", iter, ex))
          .repeat(Math.max(1, benchmarksState.settings.messagesPerExecutionInterval()))
          .doFinally(
              signalType -> {
                if (executionTaskInterval.isZero()) {
                  scheduler.schedule(this);
                } else {
                  scheduler.schedule(this, executionTaskInterval.toMillis(), TimeUnit.MILLISECONDS);
                }
              })
          .subscribe();
      return;
    }

    if (setScheduled()) { // scheduled
      scheduledCompletingTask.set(
          scheduler.schedule(
              () -> {
                LOGGER.debug(
                    "Task is completing due to execution duration limit: "
                        + scenarioDuration.toMillis());
                startCompleting();
              },
              scenarioDuration.toMillis(),
              TimeUnit.MILLISECONDS));

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
    return taskStatus.compareAndSet(null, COMPLETING)
        || taskStatus.compareAndSet(SCHEDULED, COMPLETING);
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
            empty -> setCompleted(),
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
