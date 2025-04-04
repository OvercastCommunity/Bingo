package tc.oc.bingo.objectives;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import tc.oc.pgm.api.PGM;

@Tracker("hostname-check")
public class HostNameObjective extends ObjectiveTracker {

  private final Supplier<String> REQUIRED_STRING = useConfig("required-string", "bingo");

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLogin(PlayerLoginEvent event) {
    Player player = event.getPlayer();
    if (event.getHostname().toLowerCase().contains(REQUIRED_STRING.get())) {
      PGM.get().getExecutor().schedule(() -> reward(player), 5L, TimeUnit.SECONDS);
    }
  }
}
