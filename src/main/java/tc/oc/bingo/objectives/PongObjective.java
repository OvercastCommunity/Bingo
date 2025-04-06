package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("ping-pong")
public class PongObjective extends ObjectiveTracker {

  private final Set<UUID> pinged = Collections.newSetFromMap(useState(Scope.LIFE));

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
    String[] params = event.getMessage().split(" ");
    if (params.length != 1 || params[0].length() != 5 || params[0].charAt(0) != '/') return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    if (params[0].equalsIgnoreCase("/ping")) {
      event.setCancelled(true);
      player.sendMessage(text("Pong!", NamedTextColor.YELLOW));
      pinged.add(event.getPlayer().getUniqueId());
    } else if (params[0].equalsIgnoreCase("/pong")) {
      event.setCancelled(true);
      player.sendMessage(
          text(
              "I hear " + player.getNameLegacy() + " likes cute Asian boys.",
              NamedTextColor.YELLOW));
      if (pinged.contains(player.getId())) reward(event.getPlayer());
    }
  }
}
