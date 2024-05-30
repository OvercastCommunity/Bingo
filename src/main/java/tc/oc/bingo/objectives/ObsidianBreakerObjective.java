package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

@Tracker("obsidian-breaker")
public class ObsidianBreakerObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (Material.OBSIDIAN != event.getBlock().getType()) return;
    Player player = event.getPlayer();
    if (player == null) return;

    if (Material.DIAMOND_PICKAXE != player.getItemInHand().getType()) {
      reward(player);
    }
  }
}
