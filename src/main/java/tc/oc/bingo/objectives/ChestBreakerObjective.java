package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_READER;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("chest-breaker")
public class ChestBreakerObjective extends ObjectiveTracker {

  private final Map<Vector, String> trackedBlocks = new HashMap<>();

  private final Supplier<Material> TRACKED_BLOCK =
      useConfig("tracked-block", Material.CHEST, MATERIAL_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    trackedBlocks.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    // Check if the placed block is a chest
    if (!event.getBlock().getType().equals(TRACKED_BLOCK.get())) return;

    // Get the player who placed the chest and track it
    MatchPlayer placer = getPlayer(event.getPlayer());
    if (placer == null) return;

    if (!(placer.getParty() instanceof Competitor competitor)) return;

    // Store the chest location and the player's ID
    trackedBlocks.put(event.getBlock().getLocation().toVector(), competitor.getId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    // Check if the broken block is a chest
    if (!event.getBlock().getType().equals(TRACKED_BLOCK.get())) return;

    // Get the player who broke the chest
    MatchPlayer breaker = getPlayer(event.getPlayer());
    if (breaker == null) return;

    Location chestLocation = event.getBlock().getLocation();
    String placerTeam = trackedBlocks.remove(chestLocation.toVector());
    if (placerTeam == null) return;

    if (!(breaker.getParty() instanceof Competitor competitor)) return;
    if (placerTeam.equals(competitor.getId())) return;

    reward(breaker.getBukkit());
  }
}
