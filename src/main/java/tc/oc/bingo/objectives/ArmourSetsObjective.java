package tc.oc.bingo.objectives;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.spawns.events.PlayerSpawnEvent;

@Tracker("armour-sets-crafted")
public class ArmourSetsObjective extends ObjectiveTracker {

  private final Map<UUID, int[]> setsCrafted = useState(Scope.LIFE);

  private final Supplier<Integer> REQUIRED_COUNT = useConfig("required-count", 9);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    setsCrafted.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSpawn(PlayerSpawnEvent event) {
    setsCrafted.remove(event.getPlayer().getId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraftEvent(CraftItemEvent event) {
    int index = getIronArmourIndex(event.getCurrentItem());
    if (index == -1) return;

    Player player = event.getActor();
    int[] crafted = setsCrafted.computeIfAbsent(player.getUniqueId(), (uuid) -> new int[4]);

    int craftCount = event.getRecipe().getResult().getAmount();
    // When shift clicking you can create many items at once
    if (event.getClick().equals(ClickType.SHIFT_LEFT)) {
      // Calculate how many can be made
      int minAmount =
          Arrays.stream(event.getView().getTopInventory().getContents())
              .skip(1) // Skip the first item
              .filter(item -> item != null && item.getType() != Material.AIR)
              .mapToInt(ItemStack::getAmount)
              .min()
              .orElse(0);

      // Calculate how many can fit in the inventory
      Inventory playerInventory = event.getView().getBottomInventory();
      int emptySlots =
          (int)
              IntStream.range(0, playerInventory.getSize())
                  .mapToObj(playerInventory::getItem)
                  .filter(
                      item ->
                          item == null || item.getType() == Material.AIR || item.getAmount() == 0)
                  .count();

      craftCount = Math.min(minAmount, emptySlots) * craftCount;
    }

    crafted[index] += craftCount;

    for (int count : crafted) {
      if (count < REQUIRED_COUNT.get()) return;
    }

    reward(player);
  }

  public int getIronArmourIndex(ItemStack item) {
    switch (item.getType()) {
      case IRON_HELMET:
        return 0;
      case IRON_CHESTPLATE:
        return 1;
      case IRON_LEGGINGS:
        return 2;
      case IRON_BOOTS:
        return 3;
      default:
        return -1;
    }
  }
}
