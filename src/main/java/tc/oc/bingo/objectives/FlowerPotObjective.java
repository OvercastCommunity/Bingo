package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;

@Tracker("flower-pot")
public class FlowerPotObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

    Player player = event.getPlayer();
    Block block = event.getClickedBlock();
    if (block.getType().equals(Material.FLOWER_POT)) {
      ItemStack itemInHand = player.getItemInHand();
      if (isFlower(itemInHand.getType())) {
        Match match = getMatch(event.getWorld());
        if (match == null) return;
        reward(event.getPlayer());
      }
    }
  }

  public boolean isFlower(Material material) {
    return material.equals(Material.RED_ROSE) || material.equals(Material.YELLOW_FLOWER);
  }
}
