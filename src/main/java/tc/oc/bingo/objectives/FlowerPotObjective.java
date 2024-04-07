package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;

@Tracker("flower-pot")
public class FlowerPotObjective extends ObjectiveTracker {

  // Bypass the flower-only restriction, allow bush, mushroom to work
  private boolean allowAny = false;

  @Override
  public void setConfig(ConfigurationSection config) {
    allowAny = config.getBoolean("allow-any", false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

    Player player = event.getPlayer();
    Block block = event.getClickedBlock();
    if (block.getType().equals(Material.FLOWER_POT)) {
      ItemStack itemInHand = player.getItemInHand();
      if (allowAny || isFlower(itemInHand.getType())) {
        Match match = getMatch(event.getWorld());
        if (match == null) return;
        reward(event.getPlayer());
      }
    }
  }

  public boolean isFlower(Material material) {
    switch (material) {
      case RED_ROSE:
      case YELLOW_FLOWER:
      case CACTUS:
      case SAPLING:
      case RED_MUSHROOM:
      case BROWN_MUSHROOM:
      case DEAD_BUSH:
        return true;
      default:
        return false;
    }
  }
}
