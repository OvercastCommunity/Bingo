package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("agar-io")
public class AgarObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> SIZE_NEEDED = useConfig("size-needed", 100);
  private final Supplier<Integer> SIZE_START = useConfig("starting-size", 1);
  private final Supplier<Integer> SIZE_OVERFLOW = useConfig("overflow-size", 15);

  // Inspired by the Agar.io game, players will have a score that can be absorbed by other players.
  // Everybody starts with a score of 0.1 (config value)
  // When you get a kill you absorb their current value (added on to yours and theirs reset to
  // default 0.1).
  // If a player reaches the MAX (100 config value) then they are rewarded (should their score be
  // reset to 0.1)? maybe set back to 10/20 (config value)
  // If a player dies to not a player should it reset too?

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onDeath(MatchPlayerDeathEvent event) {
    // If a player dies on their own set their progress back to the start
    if (event.getKiller() == null || event.isSelfKill() || event.isSuicide()) {
      storeObjectiveData(event.getPlayer().getId(), SIZE_START.get());
      return;
    }

    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    MatchPlayer deadPlayer = event.getPlayer();

    // Add dead player's size to killer
    trackProgress(killer.getBukkit(), getObjectiveData(deadPlayer.getId()));

    // TODO: turn in to a record not stateful int, track if player got the objective already
    // TODO: Don't reset them back to 0 if they have already been rewarded?
    if (getObjectiveData(killer.getId()) >= SIZE_NEEDED.get()) {
      storeObjectiveData(killer.getId(), SIZE_OVERFLOW.get());
    }

    storeObjectiveData(deadPlayer.getId(), SIZE_START.get());
  }

  @Override
  public @NotNull Integer initial() {
    return SIZE_START.get();
  }

  @Override
  protected int maxValue() {
    return SIZE_NEEDED.get();
  }
}
