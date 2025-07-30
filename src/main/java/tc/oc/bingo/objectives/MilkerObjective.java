package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("milker")
public class MilkerObjective extends ObjectiveTracker {

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
    Player player = event.getPlayer();
    // Check if the player is holding a bucket and milked a cow
    if (player.getItemInHand().getType() != Material.BUCKET) return;

    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    if (!(event.getRightClicked() instanceof Cow cow)) return;
    if (!cow.isAdult()) return;

    reward(player);
    // TODO: come back to check if holding a milk bucket?
  }
}
