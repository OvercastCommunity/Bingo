package tc.oc.bingo.util;

import java.util.function.BooleanSupplier;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.PGM;

public class RepeatCheckTask extends BukkitRunnable {

  private final BooleanSupplier check; // The condition to check
  private final Runnable onSuccess; // Action if the condition passes all checks
  private int maxIterations; // Number of checks to perform

  private int currentIteration = 0; // Tracks the current iteration

  public RepeatCheckTask(BooleanSupplier check, Runnable onSuccess) {
    this.check = check;
    this.onSuccess = onSuccess;
  }

  @Override
  public void run() {
    if (!check.getAsBoolean()) {
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
    return runTaskTimer(PGM.get(), 20, 20); // Schedule the task
  }

  public BukkitTask start(long intervalTicks) {
    this.maxIterations = 1;
    return runTaskTimer(PGM.get(), intervalTicks, intervalTicks); // Schedule the task
  }

  /**
   * Schedules the task with the given plugin and interval.
   *
   * @param intervalTicks The interval between checks in ticks.
   * @return The scheduled BukkitTask.
   */
  public BukkitTask start(int iterations, long intervalTicks) {
    this.maxIterations = iterations;
    return runTaskTimer(PGM.get(), intervalTicks, intervalTicks); // Schedule the task
  }
}
