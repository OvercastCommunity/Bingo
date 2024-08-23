package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.payload.Payload;
import tc.oc.pgm.regions.Bounds;

@Tracker("hill-king")
public class HillKingObjective extends ObjectiveTracker {

  private Collection<ControlPoint> hills = new ArrayList<>();
  private final Map<UUID, Integer> killCount = useState(Scope.MATCH);

  private final Supplier<Integer> MAX_HILL_SIZE = useConfig("max-hill-size", 30);
  private final Supplier<Integer> REQUIRED_KILLS = useConfig("required-kills", 6);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    hills.clear();

    GoalMatchModule goals = event.getMatch().getModule(GoalMatchModule.class);
    if (goals == null) return;

    hills =
        goals.getGoals().stream()
            .filter(goal -> goal instanceof ControlPoint)
            .filter((goal -> !(goal instanceof Payload)))
            .map(goal -> (ControlPoint) goal)
            .filter(this::isValidHill)
            .collect(Collectors.toList());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    UUID playerId = player.getId();
    Competitor playerTeam = player.getCompetitor();

    if (hills.stream()
        .filter(hill -> hill.isCompleted(playerTeam))
        .anyMatch(hill -> hill.getCaptureRegion().contains(player))) {
      int newKillCount = killCount.getOrDefault(playerId, 0) + 1;
      killCount.put(playerId, newKillCount);

      if (newKillCount >= REQUIRED_KILLS.get()) {
        reward(player.getBukkit());
      }
    }
  }

  private boolean isValidHill(ControlPoint hill) {
    Region region = hill.getCaptureRegion();
    Bounds bounds = region.getBounds();

    if (!hill.hasShowOption(ShowOption.STATS)) return false;

    Vector min = bounds.getMin();
    Vector max = bounds.getMax();

    double xSpan = Math.abs(max.getX() - min.getX());
    double zSpan = Math.abs(max.getZ() - min.getZ());

    return Math.max(xSpan, zSpan) < MAX_HILL_SIZE.get();
  }
}
