package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.util.channels.Channel;
import tc.oc.pgm.util.event.ChannelMessageEvent;

@Tracker("chat-chant")
public class ChatChantObjective extends ObjectiveTracker {

  private final Supplier<String> REQUIRED_TEXT = useConfig("required-text", "Beetlejuice");
  private final Supplier<Boolean> IGNORE_CASE = useConfig("ignore-case", false);

  private final Supplier<Integer> MIN_COUNT = useConfig("min-count", 3);

  private final List<UUID> chanters = new ArrayList<>();
  private int currentStreak = 0;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMessageSent(ChannelMessageEvent event) {
    Player player = event.getSender();
    String message = event.getMessage();

    if (player == null) return;
    if (message == null || message.isEmpty()) return;
    if (event.getChannel() != Channel.GLOBAL) return;

    // Reset the streak if not a match
    if ((!IGNORE_CASE.get() || !message.equalsIgnoreCase(REQUIRED_TEXT.get()))
        && (IGNORE_CASE.get() || !message.equals(REQUIRED_TEXT.get()))) {

      chanters.clear();
      currentStreak = 0;
      return;
    }

    chanters.add(player.getUniqueId());
    if (chanters.size() < MIN_COUNT.get()) return;

    // Get the players who haven't been rewarded in the previous streak
    List<Player> rewardingPlayers =
        chanters.stream()
            .skip(currentStreak)
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .toList();

    // Update the current streak to the new number of chanters
    currentStreak = chanters.size();

    reward(rewardingPlayers);
  }
}
