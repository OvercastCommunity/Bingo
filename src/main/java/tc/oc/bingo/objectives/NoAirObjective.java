package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.tracker.info.GenericDamageInfo;

@Tracker("no-air")
public class NoAirObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!(event.getDamageInfo() instanceof GenericDamageInfo damageInfo)) return;

    if (damageInfo.getDamageType() == EntityDamageEvent.DamageCause.DROWNING) {
      reward(event.getPlayer().getBukkit());
    }
  }
}
