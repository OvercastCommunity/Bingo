package tc.oc.bingo.objectives;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;

@Tracker("shared-monument")
public class SharedMonumentObjective extends ObjectiveTracker {

  private final Supplier<Double> MULTIPLIER = useConfig("difficulty-multiplier", 0.7);
  private final Supplier<Integer> MIN_REQUIRED_PLAYERS = useConfig("min-required-players", 3);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(GoalCompleteEvent event) {
    Goal<?> goal = event.getGoal();
    if (!(goal instanceof Destroyable)) return;
    Destroyable destroyable = (Destroyable) goal;

    int size = destroyable.getBlockRegion().getBlockVolume();
    int requiredPlayers =
        Math.max(MIN_REQUIRED_PLAYERS.get(), (int) Math.round(Math.sqrt(size) * MULTIPLIER.get()));

    List<Player> players =
        destroyable.getTouchingPlayers().stream()
            .map(MatchPlayerState::getPlayer)
            .filter(Optional::isPresent)
            .map(matchPlayer -> matchPlayer.get().getBukkit())
            .collect(Collectors.toList());

    if (players.size() >= requiredPlayers) {
      reward(players);
    }
  }
}
