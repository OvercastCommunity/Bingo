package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.card.BingoRewardEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("goal-assistance")
public class GoalAssistanceObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 10);
  private final Supplier<Integer> REWARD_DELAY = useConfig("reward-delay-seconds", 5);

  Set<UUID> rewardingPlayerQueue = new LinkedHashSet<>();
  BukkitTask rewardingTask = null;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBingoReward(BingoRewardEvent event) {
    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    if (!matchPlayer.getMatch().isRunning()) return;

    Collection<Player> nearbyPlayers =
        matchPlayer.getWorld().getNearbyPlayers(matchPlayer.getLocation(), MAX_DISTANCE.get());

    Set<Player> collect =
        nearbyPlayers.stream()
            .filter(player -> !player.equals(event.getPlayer()))
            .sorted(
                (p1, p2) -> {
                  double distance1 = p1.getLocation().distanceSquared(matchPlayer.getLocation());
                  double distance2 = p2.getLocation().distanceSquared(matchPlayer.getLocation());
                  return Double.compare(distance1, distance2);
                })
            .collect(Collectors.toCollection(LinkedHashSet::new));

    collect.forEach(
        player -> {
          if (rewardingPlayerQueue.contains(player.getUniqueId())) return;
          rewardingPlayerQueue.add(player.getUniqueId());
        });

    if (rewardingTask == null && !rewardingPlayerQueue.isEmpty()) {
      rewardingTask =
          Bukkit.getScheduler()
              .runTaskTimer(
                  Bingo.get(),
                  () -> {
                    if (rewardingPlayerQueue.isEmpty()) {
                      Bukkit.getScheduler().cancelTask(rewardingTask.getTaskId());
                      rewardingTask = null;
                      return;
                    }

                    UUID playerId = rewardingPlayerQueue.iterator().next();
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                      reward(player);
                    }

                    rewardingPlayerQueue.remove(playerId);
                    if (rewardingPlayerQueue.isEmpty()) {
                      Bukkit.getScheduler().cancelTask(rewardingTask.getTaskId());
                    }
                  },
                  20,
                  REWARD_DELAY.get() * 20);
    }
  }
}
