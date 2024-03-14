package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.spawns.events.PlayerSpawnEvent;

@Tracker("hedgehog")
public class HedgehogObjective extends ObjectiveTracker {

  public static final int MIN_ARROWS = 25;

  public HashMap<UUID, Integer> arrows = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSpawn(PlayerSpawnEvent event) {
    arrows.remove(event.getPlayer().getId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player) {
      Player actor = (Player) event.getEntity();
      if (actor.getArrowsStuck() + 1 >= MIN_ARROWS) {
        reward(actor);
      }
    }
  }
}
