package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
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
import tc.oc.pgm.api.match.event.MatchFinishEvent;

@Tracker("tall-cactus")
public class TallCactusObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_GROW_TIME = useConfig("min-grow-time", 30);
  private final Supplier<Integer> MAX_GROW_TIME = useConfig("max-grow-time", 120);

  private final Supplier<Integer> REQUIRED_HEIGHT = useConfig("required-height", 5);
  private final Supplier<Integer> MAX_HEIGHT = useConfig("max-height", 15);

  private final Map<UUID, Location> cactusOwners = useState(Scope.PARTICIPATION);

  // TODO: Cancel players trying to grow it by hand (allow up to 3)

  // TODO: (pugzy) check collision and add sounds

  // TODO: on kit apply check if item contains cactus, dont apply this logic if so

  private int growthTaskId = -1;

  private final BlockFace[] blockFaces = {
    BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCactusPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() == Material.CACTUS
        && event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SAND) {
      Location cactusLocation = event.getBlock().getLocation();
      UUID playerId = event.getPlayer().getUniqueId();
      cactusOwners.put(playerId, cactusLocation);

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
    if (cactusOwners.isEmpty()) {
      return;
    }

    // Pick a random cactus to grow
    List<UUID> players = new ArrayList<>(cactusOwners.keySet());
    UUID playerId = players.get(new Random().nextInt(players.size()));
    Location baseLocation = cactusOwners.get(playerId);

    // Ensure the base cactus still exists
    if (baseLocation.getBlock().getType() != Material.CACTUS) {
      cactusOwners.remove(playerId);
      return;
    }

    // Check the height of the cactus
    int height = 1;
    Location topLocation = baseLocation.clone().add(0, 1, 0);
    while (topLocation.getBlock().getType() == Material.CACTUS) {
      height++;
      topLocation.add(0, 1, 0);
    }

    // Ensure the next block is air and can support a new cactus
    if (canPlace(baseLocation.getBlock())) {
      topLocation.getBlock().setType(Material.CACTUS);
      topLocation.getWorld().playSound(topLocation, Sound.DIG_WOOL, 1.0f, 1.0f);
    } else {
      cactusOwners.remove(playerId);
    }

    // Reward the player when the cactus reaches 4 blocks tall
    if (height >= REQUIRED_HEIGHT.get()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        reward(player);
      }

      // Randomly stop growing once reached max height
      if (height >= MAX_HEIGHT.get() && Math.random() > 0.75) {
        cactusOwners.remove(playerId);
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
