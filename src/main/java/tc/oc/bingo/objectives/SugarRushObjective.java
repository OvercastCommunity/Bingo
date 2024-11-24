package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@Tracker("sugar-rush")
public class SugarRushObjective extends ObjectiveTracker {

  private final Supplier<Integer> FOOD_EAT_MIN = useConfig("count-required", 3);
  private final Supplier<Integer> TIME_THRESHOLD_SEC = useConfig("time-threshold-seconds", 2);

  private final Map<UUID, Long> lastConsumedTime = useState(Scope.LIFE);
  private final Map<UUID, Integer> consecutiveCount = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Material item = event.getItem().getType();
    if (item != Material.GOLDEN_APPLE) return;

    Player player = event.getPlayer();
    UUID playerUUID = player.getUniqueId();

    int updatedCount = updateConsumedCount(playerUUID);

    if (updatedCount >= FOOD_EAT_MIN.get()) {
      reward(player);
    }
  }

  private int updateConsumedCount(UUID playerUUID) {
    long currentTime = System.currentTimeMillis();
    long timeThresholdMillis = TIME_THRESHOLD_SEC.get() * 1000L;

    Long lastTime = lastConsumedTime.get(playerUUID);

    if (lastTime != null && (currentTime - lastTime <= timeThresholdMillis)) {
      consecutiveCount.put(playerUUID, consecutiveCount.getOrDefault(playerUUID, 0) + 1);
    } else {
      consecutiveCount.put(playerUUID, 1);
    }

    lastConsumedTime.put(playerUUID, currentTime);
    return consecutiveCount.get(playerUUID);
  }
}
