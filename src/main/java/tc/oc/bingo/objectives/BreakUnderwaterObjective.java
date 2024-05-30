package tc.oc.bingo.objectives;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

@Tracker("break-underwater") // TODO: Wet Work
public class BreakUnderwaterObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (Objects.isNull(player)) return;
    if (isPlayerInWater(player)) {
      reward(event.getPlayer());
    }
  }

  private boolean isPlayerInWater(Player player) {
    Material materialBottom = player.getLocation().getBlock().getType();
    Material materialTop = player.getEyeLocation().getBlock().getType();
    return (Material.STATIONARY_WATER.equals(materialBottom)
            || Material.WATER.equals(materialBottom))
        && (Material.STATIONARY_WATER.equals(materialTop) || Material.WATER.equals(materialTop));
  }
}
