package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.entity.EntityExtinguishEvent;

@Tracker("burn-out")
public class BurnOutObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExtinguish(EntityExtinguishEvent event) {
    MatchPlayer player = getPlayer(event.getEntity());
    if (player == null) return;

    double health = player.getBukkit().getHealth();
    if (health <= 1) reward(player.getBukkit());
  }
}
