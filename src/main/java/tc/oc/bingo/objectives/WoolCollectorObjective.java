package tc.oc.bingo.objectives;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
import org.jetbrains.annotations.NotNull;

@Tracker("wool-collector")
public class WoolCollectorObjective extends ObjectiveTracker.Stateful<Set<Integer>> {

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
    Set<Integer> indexes = getObjectiveData(playerId);
    if (indexes.add(woolId)) {
      storeObjectiveData(playerId, indexes);
      if (indexes.size() >= minWoolCount) {
        reward(player);
      }
    }
  }

  public @Nullable Integer getWoolId(ItemStack item) {
    if (!item.getType().equals(Material.WOOL)) return null;
    return (int) item.getDurability();
  }

  @Override
  public @NotNull Set<Integer> initial() {
    return new HashSet<>();
  }

  @Override
  public @NotNull Set<Integer> deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Arrays.stream(string.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
  }

  @Override
  public @NotNull String serialize(@NotNull Set<Integer> data) {
    return String.join(",", Iterables.transform(data, Object::toString));
  }
}
