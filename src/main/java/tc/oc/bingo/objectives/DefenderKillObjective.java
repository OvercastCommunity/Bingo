package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.goals.GoalMatchModule;

@Tracker("defender-kill")
public class DefenderKillObjective extends ObjectiveTracker {

  private int objectiveRange = 10;

  public GoalMatchModule goals = null;
  public HashMap<Competitor, Set<Vector>> objectiveLocations = new HashMap<>();

  @Override
  public void setConfig(ConfigurationSection config) {
    objectiveRange = config.getInt("objective-range", 10);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    goals = event.getMatch().getModule(GoalMatchModule.class);
    objectiveLocations.clear();

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

                        // TODO: do
                        //        if (goal instanceof OwnedGoal) {
                        //          goal.getProximityLocations();
                        //        }
                        //        goal.get

                      });
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;
    if (!killer.getPlayer().isPresent()) return;
  }
}
