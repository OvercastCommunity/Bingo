package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.bingo.modules.FreezerModule;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

@Tracker("frozen-item")
@DependsOn(FreezerModule.class)
public class FrozenItemObjective extends ObjectiveTracker {

  private final Supplier<Material> ITEM_REQUIRED = useConfig("item-required", Material.ICE);

  @EventHandler(ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getActor() instanceof Player player)) return;

    if (passesVibeCheck(event.getInventory(), event.getCurrentItem())) {
      reward(player);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onItemTransfer(PlayerItemTransferEvent event) {
    if (passesVibeCheck(event.getFrom(), event.getItem())) {
      reward(event.getPlayer());
    }
  }

  private boolean passesVibeCheck(Inventory inventory, ItemStack itemStack) {
    if (inventory == null || inventory.getSize() != 9) return false;
    if (!inventory.hasCustomName() || !inventory.getName().equals("Freezer")) return false;

    return itemStack.getType() == ITEM_REQUIRED.get();
  }
}
