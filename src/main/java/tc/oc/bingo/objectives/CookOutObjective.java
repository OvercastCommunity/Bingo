package tc.oc.bingo.objectives;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.FurnaceExtractEvent;

@Tracker("cook-out")
public class CookOutObjective extends ObjectiveTracker {

  private final Map<UUID, Long> lastCooked = useState(Scope.LIFE);

  private static final Set<Material> VALID_MEAT =
      Set.of(Material.COOKED_BEEF, Material.GRILLED_PORK);

  private final Supplier<Integer> PROXIMITY_RADIUS = useConfig("proximity-radius", 10);
  private final Supplier<Integer> TIME_WINDOW_SECONDS = useConfig("time-window-seconds", 15);

  @EventHandler(ignoreCancelled = true)
  public void onFurnaceExtractEvent(FurnaceExtractEvent event) {
    if (!VALID_MEAT.contains(event.getItemType())) return;

    Player player = event.getPlayer();
    if (player == null) return;

    UUID playerId = player.getUniqueId();
    long now = System.currentTimeMillis();
    lastCooked.put(playerId, now);

    Set<Player> group = findCookGroup(player, now);

    if (group.size() > 1) reward(group);
  }

  private Set<Player> findCookGroup(Player player, long now) {
    Set<Player> group = new HashSet<>();
    Deque<Player> queue = new ArrayDeque<>();
    Set<UUID> visited = new HashSet<>();

    queue.add(player);

    while (!queue.isEmpty()) {
      Player nextPlayer = queue.poll();
      for (Player p : nextPlayer.getLocation().getNearbyPlayers(PROXIMITY_RADIUS.get())) {
        UUID id = p.getUniqueId();
        if (visited.add(id)) {
          // If they cooked add them to the checklist
          if (recentlyCooked(p, now)) {
            group.add(p);
            queue.add(p);
          }
        }
      }
    }

    return group;
  }

  private boolean recentlyCooked(Player player, long now) {
    Long last = lastCooked.get(player.getUniqueId());
    return last != null && (now - last) <= (TIME_WINDOW_SECONDS.get() * 1000);
  }
}
