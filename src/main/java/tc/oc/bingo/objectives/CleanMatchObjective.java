package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;

@Tracker("clean-match")
public class CleanMatchObjective extends ObjectiveTracker {

  public GoalMatchModule goals = null;
  public Set<Competitor> objectivesTouched = new HashSet<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    goals = event.getMatch().getModule(GoalMatchModule.class);
    objectivesTouched.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onGoalTouch(GoalTouchEvent event) {
    if (goals == null) return;

    Collection<Competitor> competitors = goals.getCompetitors(event.getGoal());
    objectivesTouched.addAll(competitors);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onGoalComplete(GoalCompleteEvent event) {
    if (goals == null) return;

    Collection<Competitor> competitors = goals.getCompetitors(event.getGoal());
    objectivesTouched.addAll(competitors);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    if (objectivesTouched.isEmpty()) return;

    Collection<Competitor> winners = event.getMatch().getWinners();

    winners.forEach(
        competitor -> {
          if (!objectivesTouched.contains(competitor)) {
            competitor.getPlayers().forEach(player -> reward(player.getBukkit()));
          }
        });
  }
}
