package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;

@Tracker("fall-count")
public class FallCountObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> MIN_FALLS = useConfig("min-falls", 50);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    if (!(event.getDamageInfo() instanceof FallInfo fall)) return;

    if (fall.getTo().equals(FallInfo.To.VOID)) return;

    Integer count = updateObjectiveData(event.getPlayer().getId(), i -> i + 1);

    if (count >= MIN_FALLS.get()) {
      reward(killer.getBukkit());
    }
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return (double) data / MIN_FALLS.get();
  }
}
