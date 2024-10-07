package tc.oc.bingo.objectives;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.event.ChannelMessageEvent;

@Tracker("yapper")
public class YapperObjective extends ObjectiveTracker {

  private final Map<UUID, Integer> messageLength = useState(Scope.MATCH);

  private final Supplier<Integer> MIN_KILLS = useConfig("min-kills", 5);
  private final Supplier<Integer> CHARS_PER_KILL = useConfig("chars-per-kill", 100);

  private StatsMatchModule statsMatchModule;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    statsMatchModule = event.getMatch().getModule(StatsMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMessageSent(ChannelMessageEvent event) {
    if (notParticipating(event.getSender())) return;

    messageLength.compute(
        event.getSender().getUniqueId(),
        (uuid, count) -> (count == null) ? 1 : count + event.getMessage().length());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    List<Player> players =
        event.getMatch().getParticipants().stream()
            .filter(
                matchPlayer -> {
                  int kills = statsMatchModule.getPlayerStat(matchPlayer).getKills();
                  Integer messageLength = this.messageLength.getOrDefault(matchPlayer.getId(), 0);

                  // Require over the min kills and less than message length to kill ratio
                  if (kills <= MIN_KILLS.get()) return false;
                  return (messageLength / CHARS_PER_KILL.get()) < kills; // TODO: >
                })
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(players);
  }
}
