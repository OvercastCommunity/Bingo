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

    if (!params[0].equalsIgnoreCase("/pong")) return;

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null) return;

    // TODO: get the users nickname if applied
    matchPlayer.sendMessage(
        text(
            "I hear " + event.getPlayer().getName() + " likes cute Asian boys.",
            NamedTextColor.YELLOW));

    event.setCancelled(true);
    reward(event.getPlayer());
  }
}
