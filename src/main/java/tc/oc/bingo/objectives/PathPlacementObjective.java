package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.*;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

@Tracker("path-placement")
public class PathPlacementObjective extends ObjectiveTracker {

  private final Supplier<Set<Material>> REQUIRED_MATERIAL =
      useConfig(
          "block-list", Set.of(Material.RED_ROSE, Material.YELLOW_FLOWER), MATERIAL_SET_READER);

  private final Supplier<Integer> MIN_COUNT = useConfig("min-count", 5);
  private final Supplier<Integer> MIN_DISTANCE = useConfig("min-distance", 3);
  private final Supplier<Integer> REQUIRED_DISTANCE = useConfig("required-distance", 15);

  private final Map<UUID, Deque<Vector>> playerPaths = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    Vector location = event.getBlock().getLocation().toVector();

    if (!REQUIRED_MATERIAL.get().contains(event.getBlock().getType())) return;

    Deque<Vector> path = playerPaths.computeIfAbsent(playerId, k -> new LinkedList<>());

    // Check for placement too close to the last one
    if (!path.isEmpty() && path.getLast().distance(location) < MIN_DISTANCE.get()) return;

    // Add the new location to the path
    path.add(location);

    // Ensure there are enough locations to validate the path
    if (path.size() < MIN_COUNT.get()) return;

    // TODO:
    // - Update to check distance from 3 back all must be further away
    // - Sum up the collective distance once above min-count to see if its above

    // Calculate distance from the first to the last point in the path
    Vector first = path.getFirst();
    Vector last = path.getLast();

    if (first.distance(last) >= REQUIRED_DISTANCE.get()) {
      reward(event.getPlayer());
    }

    // Maintain the path's integrity by ensuring it's continuously evaluated
    while (path.size() > MIN_COUNT.get()
        && path.getFirst().distance(path.getLast()) > REQUIRED_DISTANCE.get()) {
      path.removeFirst(); // Keep the path valid and moving forward
    }
  }
}
