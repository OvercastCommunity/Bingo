package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.HashSet;
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

  public HashMap<UUID, Set<Integer>> consumedIds = new HashMap<>();

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

      Set<Integer> consumedItems = consumedIds.getOrDefault(playerUUID, new HashSet<>());
      consumedItems.add(item.getId());
      consumedIds.put(playerUUID, consumedItems);

      if (consumedItems.size() >= foodsRequired) reward(event.getPlayer());
    }
  }
}
