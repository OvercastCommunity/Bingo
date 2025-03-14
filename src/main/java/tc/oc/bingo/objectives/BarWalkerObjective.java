package tc.oc.bingo.objectives;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

@Tracker("bar-walker")
public class BarWalkerObjective extends ObjectiveTracker {

  // A play on the joke "A man walks in to a bar"... but have them walk in to an iron bar.

  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    Player player = event.getPlayer();
    Location to = event.getTo();
    Block blockAtNewLocation = to.getBlock();

    if (blockAtNewLocation.getType() == Material.IRON_BARDING) {
      Vector movement = event.getTo().toVector().subtract(event.getFrom().toVector()).normalize();
      Vector looking = player.getLocation().getDirection().setY(0).normalize();

      if (movement.dot(looking) > 0.8) { // Ensure movement is mostly in looking direction
        reward(player);
      }
    }
  }
}
