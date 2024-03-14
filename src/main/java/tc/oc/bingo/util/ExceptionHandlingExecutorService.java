package tc.oc.bingo.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

public class ExceptionHandlingExecutorService implements ExecutorService {

  private final ExecutorService executor;

  public ExceptionHandlingExecutorService(ExecutorService executor) {
    this.executor = executor;
  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    return executor.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, @NotNull TimeUnit unit)
      throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(@NotNull Callable<T> task) {
    return executor.submit(wrap(task));
  }

  @NotNull
  @Override
  public <T> Future<T> submit(@NotNull Runnable task, T result) {
    return executor.submit(wrap(task), result);
  }

  @NotNull
  @Override
  public Future<?> submit(@NotNull Runnable task) {
    return executor.submit(wrap(task));
  }

  @NotNull
  @Override
  public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return executor.invokeAll(tasks);
  }

  @NotNull
  @Override
  public <T> List<Future<T>> invokeAll(
      @NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
      throws InterruptedException {
    return executor.invokeAll(tasks, timeout, unit);
  }

  @NotNull
  @Override
  public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return executor.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(
      @NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return executor.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(@NotNull Runnable command) {
    executor.execute(wrap(command));
  }

  private Runnable wrap(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
    };
  }

  private <T> Callable<T> wrap(Callable<T> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    };
  }
}
