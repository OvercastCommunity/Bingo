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

  public HedgehogObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSpawn(PlayerSpawnEvent event) {
    arrows.remove(event.getPlayer().getId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) return;
    if (!(event.getActor() instanceof Player)) return;

    Player player = (Player) event.getActor();
    UUID playerUUID = player.getUniqueId();

    int arrowsCount = arrows.getOrDefault(playerUUID, 0);
    arrows.put(playerUUID, arrowsCount + 1);

    if (arrowsCount + 1 >= MIN_ARROWS) reward(player);
  }
}
