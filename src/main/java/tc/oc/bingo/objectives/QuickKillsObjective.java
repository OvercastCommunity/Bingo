package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("quick-kills")
public class QuickKillsObjective extends ObjectiveTracker {

  public int killsRequired = 3;
  public long timeThresholdSeconds = 5;

  private final Map<UUID, Set<Long>> lastKillTimes = new HashMap<>();

  @Override
  public void setConfig(ConfigurationSection config) {
    killsRequired = config.getInt("kills-required", 3);
    timeThresholdSeconds = config.getLong("time-threshold-seconds", 5);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    lastKillTimes.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    // Update kill count and check if the goal is reached
    int size = updateKillCount(player.getId());
    if (size >= killsRequired) {
      reward(player.getBukkit());
    }
  }

  private int updateKillCount(UUID playerUUID) {
    long currentTime = System.currentTimeMillis();
    long longestTimeAllowed = currentTime - timeThresholdSeconds * 1000;

    // Get the set of timestamps for the current player
    Set<Long> timestamps = lastKillTimes.getOrDefault(playerUUID, new HashSet<>());

    // Remove timestamps older than longestTimeAllowed
    timestamps.removeIf(timestamp -> timestamp < longestTimeAllowed);

    // Update the set of timestamps for the current player with the current time
    timestamps.add(currentTime);
    lastKillTimes.put(playerUUID, timestamps);

    return lastKillTimes.size();
  }
}
