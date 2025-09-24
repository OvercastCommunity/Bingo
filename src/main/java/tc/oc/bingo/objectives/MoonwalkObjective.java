package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

@Tracker("moon-walk")
public class MoonwalkObjective extends ObjectiveTracker {

  private final Supplier<Integer> DISTANCE = useConfig("distance", 5);
  private final Map<UUID, Integer> playerWalkingDistance = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerSneak(PlayerToggleSneakEvent event) {
    playerWalkingDistance.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (!player.isSneaking()) return;

    Vector moveDirection =
        event.getTo().toVector().subtract(event.getFrom().toVector()).normalize();
    Vector lookDirection = player.getLocation().getDirection();

    // Check if player is moving backwards
    if (moveDirection.dot(lookDirection) < -0.8) {
      int distance = playerWalkingDistance.compute(playerId, (uuid, i) -> i == null ? 1 : i + 1);

      if (distance >= DISTANCE.get()) {
        reward(player);
      }
    }
  }
}
