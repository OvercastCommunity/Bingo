package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

@Tracker("punch-wood")
public class PunchWoodObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (!isWood(event.getBlock().getType())) return;
    Player player = event.getPlayer();

    if (player.getItemInHand().getType() == Material.AIR) {
      reward(player);
    }
  }

  private boolean isWood(Material material) {
    switch (material) {
      case LOG:
      case LOG_2:
        return true;
      default:
        return false;
    }
  }
}
