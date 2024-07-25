package tc.oc.bingo.objectives;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

@Tracker("leading-stats")
public class LeadingStatsObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_MINS = useConfig("required-mins", 5);
  private final Supplier<Integer> MIN_VALUE = useConfig("min-value", 5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    StatsMatchModule statsMatchModule = event.getMatch().needModule(StatsMatchModule.class);
    if (statsMatchModule == null) return;

    Duration duration = event.getMatch().getDuration();
    if (duration.toMinutes() < REQUIRED_MINS.get()) return;

    Integer bestKills = MIN_VALUE.get();
    Integer bestStreaks = MIN_VALUE.get();
    Integer bestDeaths = MIN_VALUE.get();
    Integer bestBowShots = MIN_VALUE.get();
    Double bestDamage = (double) MIN_VALUE.get();

    List<UUID> allKills = new ArrayList<>();
    List<UUID> allStreaks = new ArrayList<>();
    List<UUID> allDeaths = new ArrayList<>();
    List<UUID> allBowShots = new ArrayList<>();
    List<UUID> allDamage = new ArrayList<>();

    for (Map.Entry<UUID, PlayerStats> mapEntry : statsMatchModule.getStats().entrySet()) {
      UUID playerUUID = mapEntry.getKey();
      PlayerStats playerStats = mapEntry.getValue();

      bestKills = updateWithStats(playerUUID, playerStats.getKills(), bestKills, allKills);
      bestStreaks =
          updateWithStats(playerUUID, playerStats.getMaxKillstreak(), bestStreaks, allStreaks);
      bestDeaths = updateWithStats(playerUUID, playerStats.getDeaths(), bestDeaths, allDeaths);
      bestBowShots =
          updateWithStats(playerUUID, playerStats.getLongestBowKill(), bestBowShots, allBowShots);
      bestDamage = updateWithStats(playerUUID, playerStats.getDamageDone(), bestDamage, allDamage);
    }

    Set<UUID> best = new HashSet<>();
    best.addAll(allKills);
    best.addAll(allStreaks);
    best.addAll(allDeaths);
    best.addAll(allBowShots);
    best.addAll(allDamage);

    List<Player> rewardingPlayers =
        best.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());

    reward(rewardingPlayers);
  }

  private <T extends Number & Comparable<T>> T updateWithStats(
      UUID playerUUID, T playerValue, T bestValue, List<UUID> allStats) {

    int result = playerValue.compareTo(bestValue);
    if (result > 0) {
      allStats.clear();
      bestValue = playerValue;
    }
    if (result >= 0) {
      allStats.add(playerUUID);
    }

    return bestValue;
  }
}
