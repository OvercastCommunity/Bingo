package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

@Tracker("jukebox-music-player")
public class JukeboxMusicPlayerObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJukeboxMusicPlay(PlayerInteractEvent event) {
    if (event.getPlayer().getItemInHand() != null
        && event.getPlayer().getItemInHand().getType().isRecord()) {
      if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
        if (!(event.getClickedBlock().getState() instanceof Jukebox jukebox)) return;
        Material record = jukebox.getPlaying();

        if (record != Material.AIR) return;

        reward(event.getPlayer());
      }
    }
  }
}
