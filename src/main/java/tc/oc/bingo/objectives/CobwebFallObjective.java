package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;

@Tracker("cobweb-fall")
public class CobwebFallObjective extends ObjectiveTracker {

  private final Supplier<Double> maxVelocity = useConfig("max-velocity", 0.1);
  private final Supplier<Integer> gracePeriodSeconds = useConfig("grace-period-seconds", 8);
  private final Map<UUID, Long> slowFallers = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (player.isOnGround() || (event.getTo().getY() > event.getFrom().getY())) {
      slowFallers.remove(player.getUniqueId());
      return;
    }

    // Check if player is in a cobweb and moving slowly downwards
    if (LocationUtils.stoodInMaterial(player.getLocation(), Material.WEB)
        && player.getVelocity().getY() == 0) {
      slowFallers.put(player.getUniqueId(), System.currentTimeMillis());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    // TODO: check that death cause is void or fall
    if (!(event.getDamageInfo() instanceof FallInfo)) return;

    MatchPlayer player = event.getPlayer();
    UUID playerId = player.getId();

    if (slowFallers.containsKey(playerId)) {
      long lastCobwebTime = slowFallers.get(playerId);
      long gracePeriodMillis = gracePeriodSeconds.get() * 1000L;

      if (System.currentTimeMillis() - lastCobwebTime <= gracePeriodMillis) {
        reward(player.getBukkit());
      }

      slowFallers.remove(playerId);
    }
  }
}
