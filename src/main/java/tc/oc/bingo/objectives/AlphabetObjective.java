package tc.oc.bingo.objectives;

import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayerState;

@Tracker("alphabet-killer")
public class AlphabetObjective extends ObjectiveTracker {

  // TODO: collect all letters of the alphabet from kills
  // OR kill someone with name starting with (persistent)

  public HashMap<Integer, MatchPlayerState> itemThrowers = new HashMap<>();
  public HashMap<MatchPlayerState, Material> equippedPieces = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {}

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {}
}
