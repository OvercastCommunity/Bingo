package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.GlobalChannel;

@Tracker("chat-message")
public class ChatMessageObjective extends ObjectiveTracker {

  private final Supplier<String> REQUIRED_TEXT = useConfig("required-text", "UwU");
  private final Supplier<Boolean> IGNORE_CASE = useConfig("ignore-case", false);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMessageSent(ChannelMessageEvent<?> event) {
    MatchPlayer player = event.getSender();
    String message = event.getMessage();

    if (player == null) return;
    if (message == null || message.isEmpty()) return;
    if (!(event.getChannel() instanceof GlobalChannel)) return;

    if ((IGNORE_CASE.get() && message.equalsIgnoreCase(REQUIRED_TEXT.get()))
        || (!IGNORE_CASE.get() && message.equals(REQUIRED_TEXT.get()))) {
      reward(player.getBukkit());
    }
  }
}
