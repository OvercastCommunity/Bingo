package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.regions.RegionMatchModule;

@Tracker("high-killer")
public class HighKillerObjective extends ObjectiveTracker.StatefulInt {

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

    MatchPlayer player = getStatePlayer(event.getKiller());
    if (player == null) return;

    if (player.getLocation().getY() >= maxBuildHeight) {
      // Due to a legacy bug, kills were increased by 2 per kill
      trackProgress(player.getBukkit(), 2);
    }
  }

  @Override
  protected int maxValue() {
    return KILLS_REQUIRED.get();
  }
}
