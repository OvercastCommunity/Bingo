package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Tracker("woo-hoo")
public class WooHooObjective extends ObjectiveTracker {

  private final Supplier<Double> MAX_RANGE = useConfig("max-range", 10.0);
  private final Supplier<Integer> MAX_TIME = useConfig("max-time-seconds", 5);

  private final Map<UUID, UUID> lastWooHooBy = useState(Scope.LIFE);
  private final Map<UUID, Long> lastWooHooTime = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWooHoo(FlowerBearerObjective.PlayerWooHooEvent event) {
    Player wooer = event.getPlayer().getBukkit();
    Player target = event.getTarget().getBukkit();

    UUID wooerId = wooer.getUniqueId();
    UUID targetId = target.getUniqueId();
    long currentTime = System.currentTimeMillis();

    // Update maps with current wooed data
    lastWooHooBy.put(targetId, wooerId);
    lastWooHooTime.put(targetId, currentTime);

    // Get nearby players within MAX_RANGE.get() of the target player
    Optional<Player> first =
        target.getLocation().getNearbyPlayers(MAX_RANGE.get()).stream()
            .filter(p -> passesVibeCheck(wooer, target, p, currentTime))
            .findFirst();

    if (first.isPresent()) reward(wooer);
  }

  private boolean passesVibeCheck(
      Player wooer, Player primary, Player secondary, long currentTime) {
    // Ignore wooer and clicked player
    if (secondary.equals(wooer) || secondary.equals(primary)) return false;

    UUID otherPlayerId = secondary.getUniqueId();

    // Player was not wooed or wooed by somebody else
    UUID lastWooerId = lastWooHooBy.get(secondary.getUniqueId());
    if (lastWooerId == null || lastWooerId != wooer.getUniqueId()) return false;

    // Check that the timestamp since last wooed
    Long otherPlayerWooHooTime = lastWooHooTime.get(otherPlayerId);
    return otherPlayerWooHooTime != null
        && (currentTime - otherPlayerWooHooTime) <= MAX_TIME.get() * 1000;
  }
}
