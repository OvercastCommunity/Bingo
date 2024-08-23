package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("lingering-potion")
public class LingeringPotionObjective extends ObjectiveTracker {

  private final Supplier<Integer> PLAYER_RADIUS = useConfig("player-radius", 5);

  // Detect a player throwing a potion or water bottle on the floor as an entity near another player

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if (!isPotionBottle(event.getItemDrop().getItemStack())) return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    BukkitTask task =
        new BukkitRunnable() {
          int i = 0;

          @Override
          public void run() {
            if (++i >= 5) this.cancel();
            // Require the item to be grounded
            if (event.getItemDrop().getVelocity().getY() != 0) return;
            if (passesVibeCheck(player, event.getItemDrop().getLocation())) {
              reward(player.getBukkit());
              this.cancel();
            }
          }
        }.runTaskTimerAsynchronously(Bingo.get(), 20, 20);
  }

  private boolean passesVibeCheck(MatchPlayer player, Location location) {
    Collection<Player> nearbyPlayers = location.getNearbyPlayers(PLAYER_RADIUS.get());
    if (nearbyPlayers.isEmpty()) return false;

    Collection<MatchPlayer> players =
        nearbyPlayers.stream()
            .map(p -> player.getMatch().getPlayer(p))
            .filter(mp -> mp != null && !mp.equals(player) && mp.canInteract())
            .toList();

    return !players.isEmpty();
  }

  public boolean isPotionBottle(ItemStack itemStack) {
    return itemStack.getItemMeta() instanceof PotionMeta;
  }
}
