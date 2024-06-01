package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.regions.RegionMatchModule;

@Tracker("high-killer")
public class HighKillerObjective extends ObjectiveTracker.Stateful<Integer> {

  private RegionMatchModule regions;

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    regions = event.getMatch().getModule(RegionMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    Integer maxBuildHeight = regions == null ? null : regions.getMaxBuildHeight();
    if (maxBuildHeight == null) return;

    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (player.getLocation().getY() >= maxBuildHeight) {
      int kills = getObjectiveData(player.getId()) + 1;

      if (kills >= KILLS_REQUIRED.get()) {
        storeObjectiveData(player.getId(), kills);
        reward(player.getBukkit());
      } else {
        kills++;
        storeObjectiveData(player.getId(), kills);
      }
    }
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.parseInt(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return data.toString();
  }

  @Override
  public double progress(Integer data) {
    return (double) data / KILLS_REQUIRED.get();
  }
}
