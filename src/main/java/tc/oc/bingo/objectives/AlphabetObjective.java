package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;

import java.util.HashMap;

public class AlphabetObjective extends ObjectiveTracker {

  // TODO: collect all letters of the alphabet from kills
  // OR kill someone with name starting with (persistent)

  public HashMap<Integer, MatchPlayerState> itemThrowers = new HashMap<>();
  public HashMap<MatchPlayerState, Material> equippedPieces = new HashMap<>();

  public AlphabetObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {

  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {

  }
}
