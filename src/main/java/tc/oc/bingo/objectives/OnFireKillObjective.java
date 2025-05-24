package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("on-fire-kill")
public class OnFireKillObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    if (!(event.getDamageInfo() instanceof MeleeInfo info)) return;
    if (!(info.getWeapon() instanceof ItemInfo)) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    if (killer.getBukkit().getFireTicks() >= 0) {
      reward(killer.getBukkit());
    }
  }
}
