package tc.oc.bingo.objectives;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.PrivateMessageChannel;

@Tracker("ghosting")
public class GhostingObjective extends ObjectiveTracker.Stateful<Set<UUID>> {

  private final Supplier<Integer> requiredMessages = useConfig("required-messages", 5);
  private final Supplier<Integer> ghostDelaySeconds = useConfig("ghost-delay-seconds", 300);

  // Map<Ghoster, Map<Ghosted, Timestamp>>
  private final Map<UUID, Map<UUID, Long>> pendingGhosts = useState(Scope.MATCH);

  @Override
  public void enable() {
    super.enable();
    // Check for new ghosts every 5 seconds
    Bingo.get()
        .getServer()
        .getScheduler()
        .runTaskTimer(
            Bingo.get(),
            () -> {
              long now = System.currentTimeMillis();
              long delay = ghostDelaySeconds.get() * 1000L;
              for (Map.Entry<UUID, Map<UUID, Long>> entry : pendingGhosts.entrySet()) {
                UUID ghosterId = entry.getKey();
                Map<UUID, Long> ghostedMap = entry.getValue();
                ghostedMap
                    .entrySet()
                    .removeIf(
                        ghostedEntry -> {
                          if (now - ghostedEntry.getValue() > delay) {
                            updateObjectiveData(
                                ghosterId,
                                confirmed -> {
                                  confirmed.add(ghostedEntry.getKey());
                                  if (confirmed.size() >= requiredMessages.get()) {
                                    Player ghoster = Bingo.get().getServer().getPlayer(ghosterId);
                                    if (ghoster != null) {
                                      reward(ghoster);
                                    }
                                  }
                                  return confirmed;
                                });
                            return true; // Remove from pending
                          }
                          return false;
                        });
              }
            },
            100L,
            100L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPrivateMessage(ChannelMessageEvent<?> event) {
    if (!(event.getChannel() instanceof PrivateMessageChannel)) return;
    PrivateMessageChannel channel = (PrivateMessageChannel) event.getChannel();

    MatchPlayer sender = event.getSender();
    MatchPlayer receiver = (MatchPlayer) event.getTarget();

    if (sender == null || receiver == null) return;

    UUID senderId = sender.getId();
    UUID receiverId = receiver.getId();

    // Record the message from sender to receiver
    pendingGhosts
        .computeIfAbsent(senderId, k -> new HashMap<>())
        .put(receiverId, System.currentTimeMillis());

    // If receiver had a pending ghost on sender, it's now void
    if (pendingGhosts.containsKey(receiverId)) {
      pendingGhosts.get(receiverId).remove(senderId);
    }

    // If receiver had a confirmed ghost on sender, remove it
    updateObjectiveData(
        receiverId,
        confirmed -> {
          confirmed.remove(senderId);
          return confirmed;
        });
  }

  @Override
  public Set<UUID> initial() {
    return new HashSet<>();
  }

  @Override
  public Set<UUID> deserialize(String string) {
    if (string.isEmpty()) return initial();
    return ImmutableSet.copyOf(string.split(",")).stream()
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  @Override
  public String serialize(Set<UUID> data) {
    return data.stream().map(UUID::toString).collect(Collectors.joining(","));
  }

  @Override
  public double progress(Set<UUID> data) {
    return (double) data.size() / requiredMessages.get();
  }
}
