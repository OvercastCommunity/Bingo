package tc.oc.bingo.objectives;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

@Tracker("kd-ratio")
public class KillDeathRatioObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_KILLS = useConfig("min-kills", 5);

  private final Supplier<Double> MIN_KD = useConfig("min-kd", 1d);
  private final Supplier<Double> MAX_KD = useConfig("max-kd", 1d);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    StatsMatchModule statsMatchModule = event.getMatch().needModule(StatsMatchModule.class);
    if (statsMatchModule == null) return;

    List<Player> rewardingPlayers =
        event.getMatch().getPlayers().stream()
            .filter(
                matchPlayer -> {
                  PlayerStats playerStat = statsMatchModule.getPlayerStat(matchPlayer);

                  int kills = playerStat.getKills();
                  if (kills < MIN_KILLS.get()) return false;

                  double kdRatio = playerStat.getKD();
                  return kdRatio >= MIN_KD.get() && kdRatio <= MAX_KD.get();
                })
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(rewardingPlayers);
  }
}
