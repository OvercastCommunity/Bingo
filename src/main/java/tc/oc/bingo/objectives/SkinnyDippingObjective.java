package tc.oc.bingo.objectives;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.util.RepeatCheckTask;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

@Tracker("skinny-dipping")
public class SkinnyDippingObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_SECONDS = useConfig("required-seconds", 10);

  private final Map<UUID, BukkitTask> swimTasks = new HashMap<>(); // To track tasks for players

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    Player player = event.getPlayer();
    if (notParticipating(player)) return;

    if (passesVibeCheck(player)) {

      RepeatCheckTask repeatCheckTask =
          new RepeatCheckTask(() -> passesVibeCheck(player), () -> reward(player));
      swimTasks.put(player.getUniqueId(), repeatCheckTask.start(5, 20));

      BukkitTask task =
          new BukkitRunnable() {
            @Override
            public void run() {
              if (passesVibeCheck(event.getPlayer())) {
                reward(event.getPlayer());
              }
              swimTasks.remove(event.getPlayer().getUniqueId());
            }
          }.runTaskLater(Bingo.get(), REQUIRED_SECONDS.get() * 20);

      swimTasks.put(event.getPlayer().getUniqueId(), task);

    } else {
      cancelSwimTask(player);
    }
  }

  // Cancel the swimming task if the player leaves the water
  private void cancelSwimTask(Player player) {
    BukkitTask task = swimTasks.remove(player.getUniqueId());
    if (task != null) {
      task.cancel();
    }
  }

  private boolean passesVibeCheck(Player player) {
    Material type = player.getLocation().getBlock().getType();
    return ((type == Material.WATER || type == Material.STATIONARY_WATER) && isNaked(player));
  }

  public boolean isNaked(Player player) {
    return Arrays.stream(player.getInventory().getArmorContents())
        .allMatch(item -> item == null || item.getType() == Material.AIR);
  }
}
