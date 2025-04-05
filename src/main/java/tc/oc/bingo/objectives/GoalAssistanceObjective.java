package tc.oc.bingo.objectives;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.card.BingoRewardEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("goal-assistance")
public class GoalAssistanceObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 10);
  private final Supplier<Integer> REWARD_DELAY = useConfig("reward-delay-seconds", 5);

  private final SequencedSet<UUID> rewardingPlayerQueue = new LinkedHashSet<>();
  private Future<?> rewardingTask = null;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBingoReward(BingoRewardEvent event) {
    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;
    if (!matchPlayer.getMatch().isRunning()) return;

    matchPlayer.getWorld().getNearbyPlayers(matchPlayer.getLocation(), MAX_DISTANCE.get()).stream()
        .filter(player -> !player.equals(event.getPlayer()))
        .sorted(
            (p1, p2) -> {
              double distance1 = p1.getLocation().distanceSquared(matchPlayer.getLocation());
              double distance2 = p2.getLocation().distanceSquared(matchPlayer.getLocation());
              return Double.compare(distance1, distance2);
            })
        .map(Player::getUniqueId)
        .forEach(rewardingPlayerQueue::add);

    if (rewardingTask != null || rewardingPlayerQueue.isEmpty()) return;

    rewardingTask =
        PGM.get()
            .getExecutor()
            .scheduleAtFixedRate(
                () -> {
                  if (!rewardingPlayerQueue.isEmpty()) {
                    UUID next = rewardingPlayerQueue.removeFirst();
                    Player player = Bukkit.getPlayer(next);
                    if (player != null) reward(player);
                  } else {
                    rewardingTask.cancel(true);
                    rewardingTask = null;
                  }
                },
                1,
                REWARD_DELAY.get(),
                TimeUnit.SECONDS);
  }
}
