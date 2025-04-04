package tc.oc.bingo.objectives;

import static tc.oc.bingo.objectives.InfectionSpreadObjective.INFECTED_GROUP;
import static tc.oc.bingo.objectives.InfectionSpreadObjective.INFECTED_PERMISSION;

import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;

@Tracker("cured")
public class CuredObjective extends ObjectiveTracker {

  private static final String PERMISSION_COMMAND = "lp user %s parent removetemp %s";

  // Detect when a player drinks a milk bucket whilst infected
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    if (event.getItem().getType() == Material.MILK_BUCKET) {
      Player player = event.getPlayer();

      // Check if the player is infected
      if (player.hasPermission(INFECTED_PERMISSION)) {
        // Remove the infected permission from the player
        String cmd = PERMISSION_COMMAND.formatted(player.getUniqueId(), INFECTED_GROUP);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

        Match match = getMatch(player.getWorld());
        if (match != null) {
          // Delay flair update event to allow command to process
          match
              .getExecutor(MatchScope.LOADED)
              .schedule(
                  () -> {
                    match.callEvent(new NameDecorationChangeEvent(player.getUniqueId()));
                    reward(player);
                  },
                  2,
                  TimeUnit.SECONDS);
        }
      }
    }
  }
}
