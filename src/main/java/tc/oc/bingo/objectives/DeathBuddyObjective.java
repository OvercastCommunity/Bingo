package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("death-buddy")
public class DeathBuddyObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 8);
  private final Supplier<Integer> MAX_TIME = useConfig("max-time-seconds", 5);

  private final Map<UUID, Long> diedAtTime = useState(Scope.PARTICIPATION);
  private final Map<UUID, Vector> diedAtLocation = useState(Scope.PARTICIPATION);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchPlayerDeath(MatchPlayerDeathEvent event) {
    MatchPlayer player = event.getPlayer();

    if (!event.isSuicide()) return;

    UUID playerId = player.getId();
    Vector location = player.getBukkit().getLocation().toVector();
    long now = System.currentTimeMillis();

    // Discount void deaths
    if (location.getY() < 0) return;

    // Record the current death
    diedAtTime.put(playerId, now);
    diedAtLocation.put(playerId, location);

    List<UUID> toRemove = new ArrayList<>();
    List<UUID> buddies = new ArrayList<>();

    for (Map.Entry<UUID, Long> entry : diedAtTime.entrySet()) {
      UUID otherPlayerId = entry.getKey();
      long otherDeathTime = entry.getValue();

      // Skip if it's the same player
      if (otherPlayerId.equals(playerId)) continue;

      // Check if the record is too old (add for removal)
      if ((now - otherDeathTime) > MAX_TIME.get() * 1000) {
        toRemove.add(otherPlayerId);
        continue;
      }

      Vector otherLocation = diedAtLocation.get(otherPlayerId);
      if (location.distance(otherLocation) <= MAX_DISTANCE.get()) {
        buddies.add(otherPlayerId);
      }
    }

    // Remove outdated entries
    for (UUID uuid : toRemove) {
      diedAtTime.remove(uuid);
      diedAtLocation.remove(uuid);
    }

    if (buddies.isEmpty()) return;

    Set<Player> list =
        buddies.stream()
            .map(this::getPlayer)
            .filter(Objects::nonNull)
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toSet());

    list.add(player.getBukkit());

    reward(list);
  }
}
