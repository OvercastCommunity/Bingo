package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.database.ProgressItem;

@Tracker("wool-collector")
public class WoolCollectorObjective extends ObjectiveTracker
    implements PersistentStore<List<Integer>> {

  public Map<UUID, List<Integer>> woolsCollected = new HashMap<>();

  private int minWoolCount = 5;

  @Override
  public void setConfig(ConfigurationSection config) {
    minWoolCount = config.getInt("min-wool-count", 5);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickUpItem(
      final PlayerPickupItemEvent event) { // TODO: add other events for pickups
    validateWoolPickUp(event.getPlayer(), event.getItem().getItemStack());
  }

  // TODO: test these
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemTransfer(InventoryMoveItemEvent event) {
    if (event.getActor() instanceof Player) {
      validateWoolPickUp((Player) event.getActor(), event.getItem());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemCraft(CraftItemEvent event) {
    validateWoolPickUp(event.getActor(), event.getRecipe().getResult());
  }

  private void validateWoolPickUp(Player player, ItemStack itemStack) {
    Integer woolId = getWoolId(itemStack);
    if (woolId == null) return;

    // TODO: permission checks here too?

    UUID playerId = player.getUniqueId();
    List<Integer> indexes = getCurrentWoolIndexes(playerId);

    if (indexes.contains(woolId)) return;

    indexes.add(woolId);
    woolsCollected.put(playerId, indexes);

    storeObjectiveData(player, getStringForStore(indexes));

    if (indexes.size() >= minWoolCount) {
      reward(player);
    }
  }

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

  @Override
  public List<Integer> getDataFromString(@Nullable String string) {
    if (string == null) return new ArrayList<>();

    return Arrays.stream(string.split(",")).map(Integer::parseInt).collect(Collectors.toList());
  }

  @Override
  public String getStringForStore(List<Integer> indexes) {
    return indexes.stream().map(Object::toString).collect(Collectors.joining(","));
  }
}
