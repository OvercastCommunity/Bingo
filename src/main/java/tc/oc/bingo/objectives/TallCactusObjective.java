package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.kits.ApplyItemKitEvent;

@Tracker("tall-cactus")
public class TallCactusObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_GROW_TIME = useConfig("min-grow-time", 30);
  private final Supplier<Integer> MAX_GROW_TIME = useConfig("max-grow-time", 120);

  private final Supplier<Integer> REQUIRED_HEIGHT = useConfig("required-height", 8);
  private final Supplier<Integer> MAX_HEIGHT = useConfig("max-height", 15);

  private final Map<UUID, Location> cactusOwners = useState(Scope.PARTICIPATION);
  private final Map<UUID, Integer> lastKnownHeight = useState(Scope.PARTICIPATION);
  private final Map<UUID, Long> nextCactusGrowth = useState(Scope.PARTICIPATION);

  private boolean enabled = true;

  private int growthTaskId = -1;

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
    // TODO: event.getMatch().getDuration() check?

    // Check if the kit contains a cactus
    boolean containsCactus =
        StreamSupport.stream(event.getItems().spliterator(), false)
            .anyMatch(item -> item.getType() == Material.CACTUS);

    if (containsCactus) {
      enabled = false;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCactusPlace(BlockPlaceEvent event) {
    if (!enabled) return;

    if (event.getBlock().getType() == Material.CACTUS
        && event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SAND) {
      Location cactusLocation = event.getBlock().getLocation();
      UUID playerId = event.getPlayer().getUniqueId();
      cactusOwners.put(playerId, cactusLocation);

      logGrowth(playerId, 1, System.currentTimeMillis());

      if (growthTaskId == -1) {
        startGrowthTask();
      }
    }
  }

  private void startGrowthTask() {
    growthTaskId =
        Bukkit.getScheduler()
            .runTaskTimer(Bingo.get(), this::tickCactusGrowth, 20L, 20L)
            .getTaskId();
  }

  private void tickCactusGrowth() {
    if (!enabled || cactusOwners.isEmpty()) {
      return;
    }

    // Pick a random cactus to grow
    List<UUID> players = new ArrayList<>(cactusOwners.keySet());
    UUID playerId = players.get(new Random().nextInt(players.size()));
    Location baseLocation = cactusOwners.get(playerId);

    // Ensure the base cactus still exists
    if (baseLocation.getBlock().getType() != Material.CACTUS) {
      reset(playerId);
      return;
    }

    // Check if the cactus is ready to grow
    long now = System.currentTimeMillis();
    Long nextGrowthTime = nextCactusGrowth.get(playerId);
    if (nextGrowthTime == null || now < nextGrowthTime) {
      return;
    }

    // Check the height of the cactus
    int height = 1;
    Location topLocation = baseLocation.clone().add(0, 1, 0);
    while (topLocation.getBlock().getType() == Material.CACTUS) {
      height++;
      // Prevent growing by hand
      Integer lastHeight = lastKnownHeight.get(playerId);
      if (lastHeight != null && lastHeight >= 3 && height > lastHeight) {
        break;
      }
      topLocation.add(0, 1, 0);
    }

    // Ensure the next block is air and can support a new cactus
    if (canPlace(topLocation.getBlock())) {
      topLocation.getBlock().setType(Material.CACTUS);
      topLocation.getWorld().playSound(topLocation, Sound.DIG_WOOL, 1.0f, 1.0f);

      // Log that the cactus has been grown
      logGrowth(playerId, height, now);
    } else {
      reset(playerId);
    }

    // Reward the player when the cactus reaches 4 blocks tall
    if (height >= REQUIRED_HEIGHT.get()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        reward(player);
      }

      // Randomly stop growing once reached max height
      if (height >= MAX_HEIGHT.get() && Math.random() > 0.75) {
        reset(playerId);
      }
    }
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    if (growthTaskId != -1) {
      Bukkit.getScheduler().cancelTask(growthTaskId);
      growthTaskId = -1;
    }
  }

  private void logGrowth(UUID playerId, int height, Long now) {
    // Log the cactus growth
    int nextGrowthTime =
        (MIN_GROW_TIME.get()
                + (int) (Math.random() * (MAX_GROW_TIME.get() - MIN_GROW_TIME.get() + 1)))
            * 1000;

    lastKnownHeight.put(playerId, height);
    nextCactusGrowth.put(playerId, now + nextGrowthTime);
  }

  private void reset(UUID playerId) {
    // Reset the cactus owner and height
    cactusOwners.remove(playerId);
    lastKnownHeight.remove(playerId);
    nextCactusGrowth.remove(playerId);
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
}
