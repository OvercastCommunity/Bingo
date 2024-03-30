package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.TouchableGoal;

@Tracker("clean-match")
public class CleanMatchObjective extends ObjectiveTracker {

  private int minParticipants;

  @Override
  public void setConfig(ConfigurationSection config) {
    minParticipants = config.getInt("min-participants", 10);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    if (event.getMatch().getParticipants().size() < minParticipants) return;

    Collection<Competitor> winners = event.getMatch().getWinners();
    if (winners.size() != 1) return;

    Competitor winner = winners.iterator().next();

    GoalMatchModule gmm = event.getMatch().getModule(GoalMatchModule.class);
    if (gmm == null) return;

    boolean validMatch = false;
    for (Goal goal : gmm.getGoals()) {
      if (goal.isRequired() && goal instanceof TouchableGoal) {
        TouchableGoal<?> touchableGoal = (TouchableGoal<?>) goal;
        if (!touchableGoal.canComplete(winner)) {
          if (touchableGoal.isCompleted() || touchableGoal.isTouched()) {
            return;
          }
          validMatch = true;
        }
      }
    }

    if (!validMatch) return;

    reward(winner.getPlayers().stream().map(MatchPlayer::getBukkit).collect(Collectors.toList()));
  }
}
