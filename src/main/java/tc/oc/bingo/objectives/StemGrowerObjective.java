package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("stem-grower")
public class StemGrowerObjective extends ObjectiveTracker {

  private final Supplier<Material> STEM_MATERIAL =
      useConfig("stem-material", Material.PUMPKIN_STEM);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    // Check if the item in hand is bone meal
    ItemStack itemInHand = event.getItem();
    if (itemInHand == null
        || itemInHand.getType() != Material.INK_SACK
        || itemInHand.getDurability() != 15) return;

    Block clickedBlock = event.getClickedBlock();
    if (clickedBlock == null || clickedBlock.getType() != STEM_MATERIAL.get()) return;

    if (clickedBlock.getData() < 3 || clickedBlock.getData() >= 7) return;

    reward(event.getPlayer());
  }
}
