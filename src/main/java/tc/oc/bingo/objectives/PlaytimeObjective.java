package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.stats.StatsMatchModule;

@Tracker("playtime")
public class PlaytimeObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_MINS = useConfig("required-mins", 120);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    StatsMatchModule stats = event.getMatch().needModule(StatsMatchModule.class);
    if (stats == null) return;
    trackProgress(
        event.getMatch().getPlayers().stream().map(MatchPlayer::getBukkit).toList(),
        p -> (int) stats.getGlobalPlayerStat(p.getUniqueId()).getTimePlayed().toMinutes());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_MINS.get();
  }
}
