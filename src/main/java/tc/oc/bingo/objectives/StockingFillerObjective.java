package tc.oc.bingo.objectives;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("stocking-filler")
public class StockingFillerObjective extends ObjectiveTracker {

  private static final Set<Material> BOOT_MATERIALS =
      Set.of(
          Material.LEATHER_BOOTS,
          Material.CHAINMAIL_BOOTS,
          Material.IRON_BOOTS,
          Material.DIAMOND_BOOTS,
          Material.GOLD_BOOTS);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPlaceInItemFrame(PlayerInteractEntityEvent event) {
    if (event.getRightClicked().getType() != EntityType.ITEM_FRAME) return;

    ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
    if (itemFrame.getItem().getType() != Material.AIR) return;

    ItemStack item = event.getPlayer().getInventory().getItemInHand();
    if (item == null) return;

    Material itemType = item.getType();
    if (!BOOT_MATERIALS.contains(itemType)) return;

    reward(event.getPlayer());
  }
}
