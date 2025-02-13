package tc.oc.bingo.util;

import java.util.function.BooleanSupplier;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;

public class RepeatCheckTask extends BukkitRunnable {

  private final BooleanSupplier check;
  private final Runnable onSuccess;
  private final @Nullable Runnable onFailure;

  private int maxIterations;
  private int currentIteration = 0;

  public RepeatCheckTask(BooleanSupplier check, Runnable onSuccess) {
    this.check = check;
    this.onSuccess = onSuccess;
    this.onFailure = null;
  }

  public RepeatCheckTask(BooleanSupplier check, Runnable onSuccess, @Nullable Runnable onFailure) {
    this.check = check;
    this.onSuccess = onSuccess;
    this.onFailure = onFailure;
  }

  @Override
  public void run() {
    if (!check.getAsBoolean()) {
      if (onFailure != null) onFailure.run(); // If the check fails report back
      cancel(); // If the check fails, stop the task
      return;
    }

    currentIteration++;

    if (currentIteration >= maxIterations) {
      onSuccess.run(); // Execute the success action
      cancel(); // Stop the task
    }
  }

  public BukkitTask start(int iterations) {
    this.maxIterations = iterations;
    return runTaskTimer(Bingo.get(), 20, 20);
  }

  public BukkitTask start(long intervalTicks) {
    this.maxIterations = 1;
    return runTaskTimer(Bingo.get(), intervalTicks, intervalTicks);
  }

  /**
   * Schedules the task with the given plugin and interval.
   *
   * @param intervalTicks The interval between checks in ticks.
   * @return The scheduled BukkitTask.
   */
  public BukkitTask start(int iterations, long intervalTicks) {
    this.maxIterations = iterations;
    return runTaskTimer(Bingo.get(), intervalTicks, intervalTicks);
  }
}
