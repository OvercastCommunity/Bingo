package tc.oc.bingo.objectives;

import com.google.common.primitives.Booleans;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

  public Map<Integer, MatchPlayerState> itemThrowers = new HashMap<>();
  public Map<UUID, boolean[]> equippedPieces = new HashMap<>();

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
    if (picker == null || picker.getId().equals(thrower.getId())) return;

    // Same team check
    Competitor competitor = picker.getCompetitor();
    if (competitor == null || !competitor.equals(thrower.getParty())) return;

    boolean[] equipment = equippedPieces.computeIfAbsent(thrower.getId(), uuid -> new boolean[4]);
    equipment[getIronArmorIndex(event.getItem())] = true;

    MatchPlayer matchPlayer = thrower.getPlayer().orElse(null);
    if (matchPlayer == null) return;

    if (!Booleans.contains(equipment, false)) {
      reward(matchPlayer.getBukkit());
    }
  }

  public boolean isIronArmor(Item item) {
    return getIronArmorIndex(item) != -1;
  }

  public int getIronArmorIndex(Item item) {
    switch (item.getItemStack().getType()) {
      case IRON_HELMET:
        return 0;
      case IRON_CHESTPLATE:
        return 1;
      case IRON_LEGGINGS:
        return 2;
      case IRON_BOOTS:
        return 3;
      default:
        return -1;
    }
  }
}
