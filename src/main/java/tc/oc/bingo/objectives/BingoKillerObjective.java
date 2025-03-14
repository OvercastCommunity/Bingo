package tc.oc.bingo.objectives;

import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.card.BingoRewardEvent;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("bingo-killer")
public class BingoKillerObjective extends ObjectiveTracker {

  private UUID lastRewarded;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBingoReward(BingoRewardEvent event) {
    lastRewarded = event.getPlayer().getUniqueId();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!(event.getKiller() == null || event.isKiller(event.getPlayer()))) return;

    // TODO: make sure you can't kill yourself to get it?? or maybe this should be allowed

    if (event.getPlayer().getId().equals(lastRewarded)) {
      reward(event.getPlayer().getBukkit());
    }
  }
}
