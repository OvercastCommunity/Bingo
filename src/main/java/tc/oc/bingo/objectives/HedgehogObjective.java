package tc.oc.bingo.objectives;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@Tracker("hedgehog")
public class HedgehogObjective extends ObjectiveTracker {

  public int minArrows = 25;

  @Override
  public void setConfig(ConfigurationSection config) {
    minArrows = config.getInt("min-arrows", 25);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player) {
      Player actor = (Player) event.getEntity();
      if (actor.getArrowsStuck() + 1 >= minArrows) {
        reward(actor);
      }
    }
  }
}
