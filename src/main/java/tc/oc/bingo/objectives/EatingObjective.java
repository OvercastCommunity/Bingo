package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

@Tracker("eating")
public class EatingObjective extends ObjectiveTracker {

  public int foodsRequired = 3;

  private final Map<UUID, Set<Material>> consumedIds = new HashMap<>();

  @Override
  public void setConfig(ConfigurationSection config) {
    foodsRequired = config.getInt("foods-required", 3);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    consumedIds.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Material item = event.getItem().getType();
    if (item.isEdible()) {
      UUID playerUUID = event.getPlayer().getUniqueId();

      Set<Material> consumedItems =
          consumedIds.computeIfAbsent(playerUUID, id -> EnumSet.noneOf(Material.class));

      if (consumedItems.add(item) && consumedItems.size() >= foodsRequired)
        reward(event.getPlayer());
    }
  }
}
