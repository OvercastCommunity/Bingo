package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("wear-skull")
public class WearSkullObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWearSkull(InventoryClickEvent event) {
    MatchPlayer player = getPlayer(event.getWhoClicked());
    if (player == null || !player.isParticipating()) return;

    if (!event.getSlotType().equals(InventoryType.SlotType.ARMOR)) return;

    if (!event.getAction().equals(InventoryAction.PLACE_ALL)) return;

    if (event.getRawSlot() == 5 && event.getCursor().getType() == Material.SKULL_ITEM) {
      reward(player.getBukkit());
    }
  }
}
