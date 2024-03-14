package tc.oc.bingo.listeners;

import static org.bukkit.Bukkit.getServer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.bingo.Bingo;

public class PlayerJoinListener implements Listener {

  private final Bingo bingo;

  public PlayerJoinListener(Bingo bingo) {
    this.bingo = bingo;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    getServer()
        .getScheduler()
        .runTaskAsynchronously(
            bingo,
            () -> {
              bingo
                  .getBingoDatabase()
                  .getCard(event.getPlayer().getUniqueId())
                  .whenComplete(
                      (bingoPlayerCard, throwable) -> {
                        bingo.getCards().put(event.getPlayer().getUniqueId(), bingoPlayerCard);
                      });
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLeave(PlayerQuitEvent event) {
    bingo.getCards().remove(event.getPlayer().getUniqueId());
  }
}
