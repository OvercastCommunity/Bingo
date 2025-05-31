package tc.oc.bingo.util;

import java.util.function.BooleanSupplier;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;

public class RepeatCheckTask extends BukkitRunnable {

  private final CheckMode mode;
  private final BooleanSupplier check;
  private final Runnable onSuccess;
  private final @Nullable Runnable onFailure;

  private int maxIterations;
  private int currentIteration = 0;

  public enum CheckMode {
    /**
     * The condition must remain true for the entire duration (e.g., player stays inside a region).
     * Fails immediately if the condition is false at any point.
     */
    CONTINUOUS,

    /**
     * The condition must be true at least once within the time period (e.g., player enters a zone
     * once). Succeeds immediately when true; fails only if never true during the time.
     */
    PASS_ONCE
  }

  public RepeatCheckTask(CheckMode mode, BooleanSupplier check, Runnable onSuccess) {
    this(mode, check, onSuccess, null);
  }

  public RepeatCheckTask(
      CheckMode mode, BooleanSupplier check, Runnable onSuccess, @Nullable Runnable onFailure) {
    this.mode = mode;
    this.check = check;
    this.onSuccess = onSuccess;
    this.onFailure = onFailure;
  }

  // Legacy fallback constructor: defaults to CONTINUOUS
  @Deprecated
  public RepeatCheckTask(BooleanSupplier check, Runnable onSuccess) {
    this(CheckMode.CONTINUOUS, check, onSuccess, null);
  }

  @Deprecated
  public RepeatCheckTask(BooleanSupplier check, Runnable onSuccess, @Nullable Runnable onFailure) {
    this(CheckMode.CONTINUOUS, check, onSuccess, onFailure);
  }

  @Override
  public void run() {
    boolean result = check.getAsBoolean();
    currentIteration++;
    boolean lastIteration = currentIteration >= maxIterations;

    switch (mode) {
      case PASS_ONCE:
        if (result) {
          succeed();
        } else if (lastIteration) {
          fail();
        }
        break;

      case CONTINUOUS:
        if (!result) {
          fail();
        } else if (lastIteration) {
          succeed();
        }
        break;
    }
  }

  private void succeed() {
    onSuccess.run();
    cancel();
  }

  private void fail() {
    if (onFailure != null) onFailure.run();
    cancel();
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
