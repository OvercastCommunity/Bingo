package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

@Tracker("fire-dose")
public class FireDoseObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
    Player player = event.getPlayer();
    Block block = event.getBlockClicked().getRelative(event.getBlockFace());

    // Check if the bucket contains water
    if (event.getBucket() != Material.WATER_BUCKET) return;

    // Check if the block extinguished was fire
    if (block.getType() != Material.FIRE) return;

    reward(player);
  }
}
