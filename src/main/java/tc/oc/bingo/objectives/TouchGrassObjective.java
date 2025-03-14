package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

@Tracker("touch-grass")
public class TouchGrassObjective extends ObjectiveTracker.Stateful<String> {

  private final Supplier<Double> GRASS_CHANCE = useConfig("grass-chance", 0d);

  private final Supplier<Double> MIN_BREAK_HOURS = useConfig("min-hour-break", 3d);
  private final Supplier<Double> MAX_BREAK_HOURS = useConfig("max-hour-break", 7d);

  @Override
  public @NotNull String initial() {
    return "";
  }

  @Override
  public @NotNull String deserialize(@NotNull String string) {
    return "";
  }

  @Override
  public @NotNull String serialize(@NotNull String data) {
    return "";
  }

  @Override
  public double progress(String data) {
    return 0;
  }

  // When a match ends update the last played time for all players

  // When a player logs check the distance between the current time and the last played time
  // If the last played time is between min-hour-break and max-hour-break then reward them after a 5
  // seconds delay

  // Add a secondary way to get the objective
  // When a player breaks a grass block (tall or regular) there's a random chance GRASS_CHANCE.get()
  // to complete the objective
}
