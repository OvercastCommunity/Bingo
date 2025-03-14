package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import tc.oc.bingo.Bingo;

@Tracker("hostname-check")
public class HostNameObjective extends ObjectiveTracker {

  private final Supplier<String> REQUIRED_STRING = useConfig("required-string", "bingo");

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLogin(PlayerLoginEvent event) {
    Player player = event.getPlayer();
    String playerAddress = event.getAddress().getHostName().toLowerCase();

    if (playerAddress.contains(REQUIRED_STRING.get())) {
      Bukkit.getScheduler()
          .runTaskLater(Bingo.get(), () -> reward(player), 100L); // 5 seconds delay
    }
  }
}
