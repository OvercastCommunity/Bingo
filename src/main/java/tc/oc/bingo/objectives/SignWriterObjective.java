package tc.oc.bingo.objectives;

import java.util.Arrays;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;

@Tracker("sign-writer")
public class SignWriterObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSignEdit(SignChangeEvent event) {
    Player player = event.getPlayer();
    String[] lines = event.getLines();

    // Trim the player name to the first 8 characters (to fit on a sign)
    String playerName =
        event.getPlayer().getName(player).substring(0, Math.min(player.getName().length(), 7));

    // Check if any of the lines contain the player's name using streams
    boolean containsPlayerName = Arrays.stream(lines).anyMatch(line -> line.contains(playerName));

    if (containsPlayerName) reward(player);
  }
}
