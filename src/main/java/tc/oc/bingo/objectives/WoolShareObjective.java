package tc.oc.bingo.objectives;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Wool;
import tc.oc.bingo.util.InventoryUtil;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

@Tracker("wool-share")
public class WoolShareObjective extends ObjectiveTracker {

  // Map of item stack UUIDs to player UUIDs
  Map<UUID, UUID> woolStacks = new HashMap<>();

  private WoolMatchModule woolMatchModule;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    woolStacks.clear();
    woolMatchModule = event.getMatch().getModule(WoolMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onItemDrop(PlayerDropItemEvent event) {
    if (woolMatchModule == null || event.getItemDrop().getItemStack().getType() != Material.WOOL)
      return;

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null) return;

    Collection<MonumentWool> monumentWools =
        woolMatchModule.getWools().get((Team) matchPlayer.getCompetitor());

    if (monumentWools.isEmpty()) return;

    BitSet woolColours =
        monumentWools.stream()
            .filter(monumentWool -> !monumentWool.isCompleted())
            .map(monumentWool -> monumentWool.getDyeColor().getWoolData())
            .collect(() -> new BitSet(16), BitSet::set, BitSet::or);

    if (woolColours.isEmpty()) return;

    Item woolItem = event.getItemDrop();
    if (!woolColours.get(woolItem.getItemStack().getData().getData())) return;

    // A player has dropped one of their wools
    woolStacks.put(woolItem.getUniqueId(), event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private void onItemPickup(PlayerPickupItemEvent event) {
    if (woolMatchModule == null || event.getItem().getItemStack().getType() != Material.WOOL)
      return;

    if (!(event.getItem().getItemStack().getData() instanceof Wool woolData)) return;

    // Check to see if the item UUID exists in the map
    UUID droppingPlayerId = woolStacks.remove(event.getItem().getUniqueId());
    if (droppingPlayerId == null) return;

    // Check if player teams match
    MatchPlayer droppingPlayer = getPlayer(droppingPlayerId);
    if (droppingPlayer == null) return;

    MatchPlayer pickupPlayer = getPlayer(event.getPlayer());
    if (pickupPlayer == null) return;

    if (droppingPlayer.equals(pickupPlayer)) return;

    if (droppingPlayer.getCompetitor() != pickupPlayer.getCompetitor()) return;

    // Ensure that the pickup player does not already have the wool in their inventory
    PlayerInventory inventory = pickupPlayer.getInventory();
    if (inventory == null) return;

    if (InventoryUtil.containsAny(
        inventory,
        it -> it.getType() == Material.WOOL && it.getData().getData() == woolData.getData())) {
      return;
    }

    reward(droppingPlayer.getBukkit());
  }
}
