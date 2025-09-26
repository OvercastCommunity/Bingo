package tc.oc.bingo.objectives;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.PrivateMessageChannel;

@Tracker("ghosting")
public class GhostingObjective extends ObjectiveTracker.StatefulSet<UUID> {

  private final Supplier<Integer> REQUIRED_MESSAGE = useConfig("required-messages", 5);
  private final Supplier<Integer> GHOSTING_DELAY = useConfig("ghost-delay-seconds", 30);

  // Map<UUID(Sender), Map<UUID(Ghoster), Timestamp>>
  private final Map<UUID, Map<UUID, Long>> pendingGhosts = useState(Scope.MATCH);

  @Override
  public Stream<ManagedListener> children() {
    return Stream.concat(
        super.children(),
        Stream.of(new ManagedListener.Ticker(this::checkGhosts, 0, 5, TimeUnit.SECONDS)));
  }

  private void checkGhosts() {
    long now = System.currentTimeMillis();
    long delay = GHOSTING_DELAY.get() * 1000L;
    for (Map.Entry<UUID, Map<UUID, Long>> entry : pendingGhosts.entrySet()) {
      UUID sender = entry.getKey();
      Map<UUID, Long> ghostedMap = entry.getValue();
      ghostedMap
          .entrySet()
          .removeIf(recipientEntry -> checkGhosting(recipientEntry, now, delay, sender));
    }
  }

  private boolean checkGhosting(
      Map.Entry<UUID, Long> recipientEntry, long now, long delay, UUID sender) {
    // Not enough time has passed
    if (now - recipientEntry.getValue() <= delay) {
      return false;
    }

    // Ensure the player is online and AFK
    MatchPlayer recipient = getPlayer(recipientEntry.getKey());
    if (recipient == null || !recipient.isActive(Duration.of(30, ChronoUnit.SECONDS))) {
      return true;
    }

    Player ghosted = Bingo.get().getServer().getPlayer(sender);
    trackProgress(ghosted, recipient.getId());

    return true;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPrivateMessage(ChannelMessageEvent<?> event) {
    if (!(event.getChannel() instanceof PrivateMessageChannel)) return;

    MatchPlayer sender = event.getSender();
    MatchPlayer receiver = (MatchPlayer) event.getTarget();

    if (!receiver.isActive(Duration.of(30, ChronoUnit.SECONDS))) return;

    if (sender == null) return;

    UUID senderId = sender.getId();
    UUID receiverId = receiver.getId();

    // Record the message from sender to receiver
    pendingGhosts
        .computeIfAbsent(senderId, k -> new HashMap<>())
        .put(receiverId, System.currentTimeMillis());

    // If receiver had a pending ghost on sender, it's now void
    Map<UUID, Long> ghosts = pendingGhosts.get(receiverId);
    if (ghosts != null) {
      ghosts.remove(senderId);
    }
  }

  @Override
  protected int maxCount() {
    return REQUIRED_MESSAGE.get();
  }

  protected UUID deserializeElement(String string) {
    return UUID.fromString(string);
  }
}
