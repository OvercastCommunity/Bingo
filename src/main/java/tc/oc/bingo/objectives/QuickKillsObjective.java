package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("quick-kills")
public class QuickKillsObjective extends ObjectiveTracker {

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 3);
  private final Supplier<Integer> TIME_THRESHOLD_SEC = useConfig("time-threshold-seconds", 5);
  private final Map<UUID, List<Long>> lastKillTimes = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer player = getStatePlayer(event.getKiller());
    if (player == null) return;

    // Update kill count and check if the goal is reached
    int size = updateKillCount(player.getId());
    if (size >= KILLS_REQUIRED.get()) {
      reward(player.getBukkit());
    }
  }

  private int updateKillCount(UUID playerUUID) {
    long currentTime = System.currentTimeMillis();
    long longestTimeAllowed = currentTime - (TIME_THRESHOLD_SEC.get() * 1000L);

    // Get the set of timestamps for the current player
    List<Long> timestamps = lastKillTimes.computeIfAbsent(playerUUID, uuid -> new ArrayList<>());

    // Remove timestamps older than longestTimeAllowed
    timestamps.removeIf(timestamp -> timestamp < longestTimeAllowed);

    // Update the set of timestamps for the current player with the current time
    timestamps.add(currentTime);

    return timestamps.size();
  }
}
