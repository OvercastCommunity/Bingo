package tc.oc.bingo.objectives;

import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;

@Tracker("armour-shared")
public class ArmourSharedObjective extends ObjectiveTracker {

  public HashMap<Integer, MatchPlayerState> itemThrowers = new HashMap<>();
  public HashMap<MatchPlayerState, Material> equippedPieces = new HashMap<>();

  public ArmourSharedObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    itemThrowers.clear();
    equippedPieces.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if (!isIronArmor(event.getItemDrop())) return;

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    itemThrowers.put(event.getItemDrop().getEntityId(), player.getState());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickUp(PlayerPickupItemEvent event) {
    if (!isIronArmor(event.getItem())) return;

    MatchPlayerState thrower = itemThrowers.getOrDefault(event.getItem().getEntityId(), null);
    if (thrower == null) return;
    itemThrowers.remove(event.getItem().getEntityId());

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    MatchPlayer picker = match.getPlayer(event.getPlayer());
    if (picker == null) return;

    // Same team check
    Competitor competitor = picker.getCompetitor();
    if (competitor == null || competitor.equals(thrower.getParty())) return;

    equippedPieces.put(thrower, event.getItem().getItemStack().getType());

    MatchPlayer matchPlayer = thrower.getPlayer().orElse(null);
    if (matchPlayer == null) return;

    if (equippedPieces.size() >= 4) reward(matchPlayer.getBukkit());
  }

  public boolean isIronArmor(Item item) {
    switch (item.getItemStack().getType()) {
      case IRON_HELMET:
      case IRON_CHESTPLATE:
      case IRON_LEGGINGS:
      case IRON_BOOTS:
        return true;
      default:
        return false;
    }
  }
}
