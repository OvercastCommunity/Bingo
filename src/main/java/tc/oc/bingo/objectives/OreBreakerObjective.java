package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import com.google.common.collect.EvictingQueue;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

@Tracker("ore-breaker")
public class OreBreakerObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_BREAKS = useConfig("required-breaks", 1000);

  private final Map<UUID, EvictingQueue<Vector>> placedBlocks = useState(Scope.PARTICIPATION);

  private final Supplier<Set<Material>> TARGET_BLOCKS =
      useConfig(
          "target-blocks",
          Set.of(
              Material.COAL_ORE,
              Material.IRON_ORE,
              Material.GOLD_ORE,
              Material.DIAMOND_ORE,
              Material.EMERALD_ORE,
              Material.LAPIS_ORE,
              Material.REDSTONE_ORE,
              Material.QUARTZ_ORE),
          MATERIAL_SET_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    if (!TARGET_BLOCKS.get().contains(event.getBlock().getType())) return;

    // Ignore blocks that were recently placed by the player
    if (placedBlocks.containsKey(player.getUniqueId())) {
      EvictingQueue<Vector> placed = placedBlocks.get(player.getUniqueId());
      Vector placedBlockLocation = event.getBlock().getLocation().toVector();
      if (placed.contains(placedBlockLocation)) return;
    }

    trackProgress(player);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    if (!TARGET_BLOCKS.get().contains(event.getBlock().getType())) return;

    // Track the location of the last `x` blocks a player placed
    // TODO: ensure in and out order is good
    placedBlocks
        .computeIfAbsent(player.getUniqueId(), id -> EvictingQueue.create(5))
        .add(event.getBlock().getLocation().toVector());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_BREAKS.get();
  }
}
