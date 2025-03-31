package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("ping-pong")
public class PongObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {

    String[] params = event.getMessage().split(" ");
    if (params.length != 1) return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    if (params[0].equalsIgnoreCase("/ping")) {
      event.setCancelled(true);
      player.sendMessage(text("Pong!", NamedTextColor.YELLOW));
    }

    if (!params[0].equalsIgnoreCase("/pong")) {
      // TODO: get the users nickname if applied
      player.sendMessage(
          text(
              "I hear " + player.getNameLegacy() + " likes cute Asian boys.",
              NamedTextColor.YELLOW));
      event.setCancelled(true);
      reward(event.getPlayer());
    }
  }
}
