package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.kits.ApplyItemKitEvent;
import tc.oc.pgm.util.StreamUtils;

@Tracker("tall-cactus")
public class TallCactusObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_GROW_TIME = useConfig("min-grow-time", 45);
  private final Supplier<Integer> MAX_GROW_TIME = useConfig("max-grow-time", 75);
  private final Supplier<Integer> GROWTH_SPREAD =
      useComputedConfig(() -> MAX_GROW_TIME.get() - MIN_GROW_TIME.get());

  private final Supplier<Integer> REQUIRED_HEIGHT = useConfig("required-height", 8);
  private final Supplier<Integer> MAX_HEIGHT = useConfig("max-height", 15);

  private final Map<UUID, CactusTracker> cactusOwners = useState(Scope.PARTICIPATION);

  private boolean enabled = true;

  private Future<?> growthTask;

  private final BlockFace[] blockFaces = {
    BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  @EventHandler
  public void onMatchLoad(MatchAfterLoadEvent event) {
    enabled = true;
  }

  @EventHandler
  public void onKitApply(ApplyItemKitEvent event) {
    if (!enabled) return;

    // Check if the kit contains a cactus
    boolean containsCactus =
        StreamUtils.of(event.getItems()).anyMatch(item -> item.getType() == Material.CACTUS);

    if (containsCactus) enabled = false;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCactusPlace(BlockPlaceEvent event) {
    if (!enabled) return;

    Block block = event.getBlock();
    if (block.getType() != Material.CACTUS
        || block.getRelative(BlockFace.DOWN).getType() != Material.SAND) return;
    cactusOwners.put(event.getPlayer().getUniqueId(), new CactusTracker(block, 1, nextGrowthBy()));

    if (growthTask == null) {
      growthTask =
          PGM.get()
              .getExecutor()
              .scheduleAtFixedRate(this::tickCactusGrowth, 1, 50, TimeUnit.MILLISECONDS);
    }
  }

  private void cancelTask() {
    if (growthTask != null) {
      growthTask.cancel(false);
      growthTask = null;
    }
  }

  private void tickCactusGrowth() {
    if (!enabled || cactusOwners.isEmpty()) cancelTask();
    else cactusOwners.entrySet().removeIf(e -> tickCactus(e.getKey(), e.getValue()));
  }

  // True if this cactus is done (fully grown, or should otherwise cancel)
  private boolean tickCactus(UUID playerId, CactusTracker tracker) {
    // Ensure the base cactus still exists
    if (tracker.base.getType() != Material.CACTUS) return true;
    if (tracker.growBy > System.currentTimeMillis()) return false;

    Block checking = tracker.base;
    int height = 0, overheight = Math.max(3, tracker.lastHeight) + 1;
    while (checking.getType() == Material.CACTUS && height <= overheight) {
      checking = checking.getRelative(BlockFace.UP);
      height++;
    }
    // Cactus was manually grown or can't grow, cancel
    if (height >= overheight || !canPlace(checking)) return true;

    height++;
    checking.setType(Material.CACTUS);
    checking.getWorld().playSound(checking.getLocation(), Sound.DIG_WOOL, 1.0f, 1.0f);

    tracker.lastHeight = height;
    tracker.growBy = nextGrowthBy();

    // Reward the player when the cactus reaches 4 blocks tall
    if (height >= REQUIRED_HEIGHT.get()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) reward(player);

      // Randomly stop growing once reached max height
      return height > MAX_HEIGHT.get() || Math.random() > 0.85;
    }
    return false;
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    cancelTask();
  }

  private long nextGrowthBy() {
    return System.currentTimeMillis()
        + ((int) (MIN_GROW_TIME.get() + (Math.random() * GROWTH_SPREAD.get())) * 1000L);
  }

  private boolean canPlace(Block location) {
    // Check that all blocks around are air too
    for (BlockFace face : blockFaces) {
      Block adjacentBlock = location.getRelative(face);
      if (adjacentBlock.getType() != Material.AIR) {
        return false;
      }
    }
    return true;
  }

  @Data
  @AllArgsConstructor
  private static class CactusTracker {
    final Block base;
    int lastHeight;
    long growBy;
  }
}
