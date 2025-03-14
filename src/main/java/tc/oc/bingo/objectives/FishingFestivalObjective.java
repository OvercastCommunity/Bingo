package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

@Tracker("fishing-festival")
public class FishingFestivalObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 10);
  private final Supplier<Integer> MAX_TIME_WINDOW = useConfig("max-time-window", 5);

  private final Supplier<Integer> FESTIVAL_RANGE = useConfig("festival-range", 25);
  private final Supplier<Integer> FESTIVAL_TIME = useConfig("festival-seconds", 60);
  private final Supplier<Integer> FESTIVAL_COOLDOWN = useConfig("festival-cooldown-seconds", 300);

  private final Map<UUID, Vector> playerCatchLocations = useState(Scope.LIFE);
  private final Map<UUID, Long> playerCatchTimestamps = useState(Scope.LIFE);

  private Vector festivalLocation = null;
  private Long festivalEndTime = null;
  private BukkitTask countdownTask = null;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    festivalLocation = null;
    festivalEndTime = null;
    Bukkit.getScheduler().cancelTask(countdownTask.getTaskId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
      Player player = event.getPlayer();
      UUID playerId = player.getUniqueId();
      Vector location = player.getLocation().toVector();
      long currentTime = System.currentTimeMillis();

      // Store the latest fishing catch for this player
      playerCatchLocations.put(playerId, location);
      playerCatchTimestamps.put(playerId, currentTime);

      // If festivalLocation is not null, check if the player is within FESTIVAL_RANGE and reward
      // them
      if (festivalLocation != null && location.distance(festivalLocation) <= FESTIVAL_RANGE.get()) {
        // TODO: random chance to set specific loot
        reward(player);
        return;
      }

      // Festival is on a cooldown
      if (festivalEndTime != null
          && currentTime - festivalEndTime <= FESTIVAL_COOLDOWN.get() * 1000) {
        return;
      }

      // TODO: Require a catch to be in non-flowing water
      // event.getHook().getLocation().getBlock();

      // Check for a match with another player
      for (UUID otherId : playerCatchTimestamps.keySet()) {
        if (!otherId.equals(playerId)) {
          Vector otherLoc = playerCatchLocations.get(otherId);
          Long otherTime = playerCatchTimestamps.get(otherId);

          if (otherLoc != null
              && otherTime != null
              && location.distance(otherLoc) <= MAX_DISTANCE.get()
              && currentTime - otherTime <= MAX_TIME_WINDOW.get() * 1000) {

            // When two players meet this criteria:
            // Set the festival location as the midpoint between their catches
            festivalLocation = otherLoc.getMidpoint(location);

            // Broadcast a message announcing the start of a fishing festival
            // TODO: change to player names
            Bukkit.broadcastMessage(
                ChatColor.GOLD
                    + "A fishing festival has started! Catch fish near "
                    + festivalLocation.toString()
                    + "!");

            // Start a countdown that resets the festival location after FESTIVAL_TIME seconds
            countdownTask =
                Bukkit.getScheduler()
                    .runTaskLater(
                        Bingo.get(),
                        () -> {
                          Bukkit.broadcastMessage(
                              ChatColor.GOLD + "The fishing festival has concluded!");
                          festivalEndTime = System.currentTimeMillis();
                          festivalLocation = null;
                          countdownTask = null;
                        },
                        FESTIVAL_TIME.get() * 20L);

            // Do not reward yet.
            return;
          }
        }
      }
    }
  }
}
