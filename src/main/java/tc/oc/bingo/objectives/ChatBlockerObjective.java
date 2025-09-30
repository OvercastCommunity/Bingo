package tc.oc.bingo.objectives;

import java.util.Locale;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.GlobalChannel;
import tc.oc.pgm.channels.TeamChannel;
import tc.oc.pgm.util.MatchPlayers;

@Tracker("chat-blocker")
public class ChatBlockerObjective extends ObjectiveTracker {

  private final Supplier<String> REQUIRED_TEXT = useConfig("required-text", "trick or treat");

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMessageSent(ChannelMessageEvent<?> event) {
    MatchPlayer player = event.getSender();
    String message = event.getMessage();

    if (!MatchPlayers.canInteract(player)) return;
    if (message == null || message.isEmpty()) return;
    Channel<?> channel = event.getChannel();

    // Require message in team or global channels
    if (!(channel instanceof GlobalChannel) && !(channel instanceof TeamChannel)) return;

    String lowerCaseMessage = message.toLowerCase(Locale.ROOT);

    if (!lowerCaseMessage.contains(REQUIRED_TEXT.get().toLowerCase(Locale.ROOT))) return;

    if (passesVibeCheck(event.getSender().getBukkit())) {
      reward(player.getBukkit());
    }
  }

  public boolean passesVibeCheck(Player player) {
    for (int x = -3; x <= 3; x++) {
      for (int z = -3; z <= 3; z++) {
        for (int y = 0; y <= 2; y++) {
          MaterialData materialData =
              player.getLocation().add(x, y, z).getBlock().getState().getMaterialData();
          if (materialData instanceof Door) {
            return true;
          }
        }
      }
    }

    return false;
  }
}
