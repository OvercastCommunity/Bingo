package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@Tracker("hedgehog")
public class HedgehogObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_ARROWS = useConfig("min-arrows", 25);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player) {
      Player actor = (Player) event.getEntity();
      if (actor.getArrowsStuck() + 1 >= MIN_ARROWS.get()) {
        reward(actor);
      }
    }
  }
}
