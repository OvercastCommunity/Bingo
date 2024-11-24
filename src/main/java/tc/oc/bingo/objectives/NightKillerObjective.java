package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("night-killer")
public class NightKillerObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    long time = event.getMatch().getWorld().getTime();
    if (time < 13000 || time > 23000) return;

    if (!event.isChallengeKill() || event.getKiller() == null) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    Integer kills = updateObjectiveData(killer.getId(), i -> i + 1);
    if (kills >= KILLS_REQUIRED.get()) reward(killer.getBukkit());
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
    return data.toString();
  }

  @Override
  public double progress(Integer data) {
    return data;
  }
}
