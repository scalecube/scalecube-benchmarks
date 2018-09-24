package io.scalecube.benchmarks;

import static io.scalecube.benchmarks.BenchmarkTaskImpl.Status.COMPLETED;
import static io.scalecube.benchmarks.BenchmarkTaskImpl.Status.COMPLETING;
import static io.scalecube.benchmarks.BenchmarkTaskImpl.Status.SCHEDULED;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class BenchmarkTaskImpl implements BenchmarkTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkTaskImpl.class);

  public enum Status {
    SCHEDULED,
    COMPLETING,
    COMPLETED
  }

  private final BiFunction<Long, BenchmarkTask, Publisher<?>> unitOfWork;
  private final Supplier<Mono<Void>> cleanUp;
  private final Scheduler scheduler;
  private final BenchmarkSettings settings;

  private final AtomicLong iterationsCounter = new AtomicLong();
  private final AtomicReference<Status> taskStatus = new AtomicReference<>();
  private final CompletableFuture<Void> taskCompletionFuture = new CompletableFuture<>();
  private final AtomicReference<Disposable> scheduledCompletingTask = new AtomicReference<>();

  /**
   * Constructs benchmark task.
   *
   * @param settings a settings.
   * @param scheduler a scheduler.
   * @param unitOfWork a unit of work.
   * @param cleanUp a function that should clean up resources.
   */
  public BenchmarkTaskImpl(
      BenchmarkSettings settings,
      Scheduler scheduler,
      BiFunction<Long, BenchmarkTask, Publisher<?>> unitOfWork,
      Supplier<Mono<Void>> cleanUp) {
    this.settings = settings;
    this.unitOfWork = unitOfWork;
    this.cleanUp = cleanUp;
    this.scheduler = scheduler;
  }

  @Override
  public BenchmarkSettings settings() {
    return settings;
  }

  @Override
  public Scheduler scheduler() {
    return scheduler;
  }

  @Override
  public void run() {
    if (isCompleted()) {
      return; // this is the end
    }

    if (iterationsCounter.get() >= settings.numOfIterations()) {
      LOGGER.debug("Task is completed due to iterations limit: " + settings.numOfIterations());
      startCompleting();
      return; // this is the end
    }

    if (isScheduled()) { // executing
      long iter = iterationsCounter.incrementAndGet();

      Publisher<?> publisher = unitOfWork.apply(iter, this);
      if (publisher instanceof Mono) {
        ((Mono<?>) publisher).subscribe();
      } else if (publisher instanceof Flux) {
        ((Flux<?>) publisher).subscribe();
      }

      return;
    }

    if (setScheduled()) { // scheduled
      // once scheduled set a cleaner
      scheduledCompletingTask.set(
          scheduler.schedule(
              () -> {
                LOGGER.debug(
                    "Task is completing due to execution duration limit: "
                        + settings.executionTaskDuration().toMillis());
                startCompleting();
              },
              settings.executionTaskDuration().toMillis(),
              TimeUnit.MILLISECONDS));

      LOGGER.debug("Obtained setUp result, now scheduling");
      scheduler.schedule(this); // now really scheduled
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
        cleanUp
            .get()
            .subscribe(
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
