package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.database.ProgressItem;

@Tracker("wool-collector")
public class WoolCollectorObjective extends ObjectiveTracker {

  public HashMap<UUID, List<Integer>> woolsCollected = new HashMap<>();

  private int minWoolCount = 5;

  //  @Override
  //  public void setConfig(ConfigurationSection config) {
  //    minWoolCount = config.getInt("min-wool-count", 5);
  //  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerOnGroundChanged(final PlayerPickupItemEvent event) {

    Integer woolId = getWoolId(event.getItem().getItemStack());
    if (woolId == null) return;

    UUID playerId = event.getPlayer().getUniqueId();
    List<Integer> indexes = getCurrentWoolIndexes(playerId);

    if (indexes.contains(woolId)) return;

    indexes.add(woolId);
    woolsCollected.put(playerId, indexes);

    if (indexes.size() >= minWoolCount) {
      reward(event.getPlayer());
    } else {
      String dataAsString = indexes.stream().map(Object::toString).collect(Collectors.joining(","));
      storeObjectiveData(event.getPlayer(), dataAsString);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemTransfer(InventoryMoveItemEvent event) {}

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemCraft(PrepareItemCraftEvent event) {}

  public @Nullable Integer getWoolId(ItemStack item) {
    if (!item.getType().equals(Material.WOOL)) return null;
    return (int) item.getDurability();
  }

  public List<Integer> getCurrentWoolIndexes(UUID playerId) {
    // Create or fetch progress item to cache
    if (!woolsCollected.containsKey(playerId)) {
      List<Integer> indexes = new ArrayList<>();
      ProgressItem progressItem = getProgress(playerId);

      if (progressItem != null) {
        indexes = getDataFromString(progressItem.getData());
      }

      woolsCollected.put(playerId, indexes);
      return indexes;
    }

    return woolsCollected.get(playerId);
  }

  public List<Integer> getDataFromString(@Nullable String string) {
    if (string == null) return new ArrayList<>();

    return Arrays.stream(string.split(",")).map(Integer::parseInt).collect(Collectors.toList());
  }
}
