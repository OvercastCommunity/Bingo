package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;

// Bingo objective awarded for surviving a fall with "x" hp remaining at a height of "y" blocks.
@Tracker("fall-damage-taken")
public class FallDamageObjective extends ObjectiveTracker {

  private final Map<UUID, Double> lastYPosition = useState(Scope.LIFE);

  private final Supplier<Integer> MIN_FALL = useConfig("min-fall-height", 20);
  private final Supplier<Double> MAX_HEALTH = useConfig("max-fall-health-remaining", 1.0);

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onFallDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) return;

    if (!event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) return;

    Player player = (Player) event.getEntity();

    double yValue = lastYPosition.get(player.getUniqueId());

    int fallHeight = (int) Math.ceil((yValue - event.getEntity().getLocation().getY()));
    double totalDamage = event.getFinalDamage();

    if (fallHeight < MIN_FALL.get()) return;

    double remainingHealth = player.getHealth() - totalDamage;

    if (remainingHealth <= MAX_HEALTH.get() && remainingHealth > 0) {
      reward((Player) event.getEntity());
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {
    if (event.getOnGround()) return;
    UUID playerId = event.getPlayer().getUniqueId();
    double y = event.getPlayer().getLocation().getY();
    lastYPosition.put(playerId, y);
  }
}
