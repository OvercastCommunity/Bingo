package tc.oc.bingo.listeners;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.util.Exceptions;
import tc.oc.pgm.api.PGM;

public class PlayerJoinListener implements Listener {

  private final Bingo bingo;

  public PlayerJoinListener(Bingo bingo) {
    this.bingo = bingo;
    Bukkit.getServer().getPluginManager().registerEvents(this, bingo);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    Exceptions.handle(
        bingo
            .loadPlayerCard(uuid)
            .thenAcceptAsync(
                (card) -> {
                  // If player logged off before database responded, remove now
                  if (!event.getPlayer().isOnline()) bingo.getCards().remove(uuid);
                },
                PGM.get().getExecutor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLeave(PlayerQuitEvent event) {
    bingo.getCards().remove(event.getPlayer().getUniqueId());
  }
}
