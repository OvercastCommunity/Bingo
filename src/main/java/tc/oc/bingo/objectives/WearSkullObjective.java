package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

@Tracker("wear-skull")
public class WearSkullObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWearSkull(InventoryClickEvent event) {
    if (notParticipating(event.getWhoClicked())) return;

    if (!event.getSlotType().equals(InventoryType.SlotType.ARMOR)) return;

    if (!event.getAction().equals(InventoryAction.PLACE_ALL)) return;

    if (event.getRawSlot() == 5 && event.getCursor().getType() == Material.SKULL_ITEM) {
      reward((Player) event.getWhoClicked());
    }
  }
}
