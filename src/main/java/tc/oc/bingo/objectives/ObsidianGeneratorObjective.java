package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.util.material.Materials;

@Tracker("obsidian-generator")
public class ObsidianGeneratorObjective extends ObjectiveTracker {

  private static final int DISTANCE_SQUARED = 6 * 6;
  private final Map<UUID, Location> waterSourcesPlaced = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockTransform(BlockTransformEvent event) {
    // Lava transformed from terrain, not from player
    if (Material.STATIONARY_LAVA.equals(event.getBlock().getType())
        && event.getCause() instanceof BlockFormEvent
        && Material.OBSIDIAN.equals(event.getNewState().getMaterial())) {
      Player player = getNearbyPlayerPlacedWater(event);
      if (player != null) reward(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    if (!Material.WATER_BUCKET.equals(event.getBucket())) return;

    // Don't allow placing water in water
    Block relative = event.getBlockClicked().getRelative(event.getBlockFace());
    if (Materials.isWater(relative.getType())) return;

    Location location = relative.getLocation().toCenterLocation();
    location.setY(Math.floor(location.getY()));
    waterSourcesPlaced.put(event.getPlayer().getUniqueId(), location);
  }

  private Player getNearbyPlayerPlacedWater(BlockTransformEvent event) {
    Block block = event.getBlock();
    for (Map.Entry<UUID, Location> entry : waterSourcesPlaced.entrySet()) {
      if (isBlockNearLocation(block, entry.getValue())) return Bukkit.getPlayer(entry.getKey());
    }
    return null;
  }

  private boolean isBlockNearLocation(Block block, Location location) {
    return (location.distanceSquared(block.getLocation()) < DISTANCE_SQUARED);
  }
}
