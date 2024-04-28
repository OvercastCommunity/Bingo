package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.teams.Team;

@Tracker("defender-kill")
public class DefenderKillObjective extends ObjectiveTracker {

  private final Supplier<Integer> OBJECTIVE_MAX_RANGE = useConfig("objective-max-range", 5);
  private final Supplier<Integer> OBJECTIVE_MAX_SIZE = useConfig("objective-max-size", 40);

  private final Map<Competitor, List<Bounds>> objectiveLocations = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    objectiveLocations.clear();
    GoalMatchModule goals = event.getMatch().getModule(GoalMatchModule.class);
    if (goals == null) return;

    event
        .getMatch()
        .getCompetitors()
        .forEach(
            competitor -> {
              goals
                  .getGoals(competitor)
                  .forEach(
                      goal -> {
                        if (goal instanceof Destroyable) {
                          Destroyable destroyable = (Destroyable) goal;
                          FiniteBlockRegion blockRegion = destroyable.getBlockRegion();
                          if (blockRegion.getBlockVolume() > OBJECTIVE_MAX_SIZE.get()) {
                            return;
                          }
                          addTeamBounds(destroyable.getOwner(), blockRegion);
                        }

                        if (goal instanceof Core) {
                          Core core = (Core) goal;
                          addTeamBounds(core.getOwner(), core.getCasingRegion());
                        }
                      });
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || objectiveLocations.isEmpty()) return;

    MatchPlayer player = getStatePlayer(event.getKiller());
    if (player == null) return;

    Vector deathLocation = event.getPlayer().getBukkit().getEyeLocation().toVector();

    if (objectiveLocations.getOrDefault(player.getCompetitor(), Collections.emptyList()).stream()
        .anyMatch(bounds -> bounds.contains(deathLocation))) {
      reward(player.getBukkit());
    }
  }

  private void addTeamBounds(Team owner, FiniteBlockRegion blockRegion) {
    List<Bounds> teamBounds = objectiveLocations.computeIfAbsent(owner, c -> new ArrayList<>());

    Bounds clonedRegion = blockRegion.getBounds().clone();
    teamBounds.add(expandBounds(clonedRegion, OBJECTIVE_MAX_RANGE.get()));
  }

  private Bounds expandBounds(Bounds bounds, int increase) {
    Vector vectorModification = new Vector(increase, increase, increase);
    Vector min = bounds.getMin().clone();
    min.subtract(vectorModification);
    Vector max = bounds.getMax().clone();
    max.add(vectorModification);
    return new Bounds(min, max);
  }
}
