package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.bingo.util.RepeatCheckTask;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

@Tracker("lava-chiller")
public class LavaChillerObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_SECONDS = useConfig("required-seconds", 10);

  private final Map<UUID, BukkitTask> swimTasks = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    Player player = event.getPlayer();
    MatchPlayer matchPlayer = getParticipant(player);
    if (!MatchPlayers.canInteract(matchPlayer)) return;

    if (swimTasks.containsKey(player.getUniqueId())) return;

    if (passesVibeCheck(player)) {

      RepeatCheckTask repeatCheckTask =
          new RepeatCheckTask(
              RepeatCheckTask.CheckMode.CONTINUOUS,
              () -> passesVibeCheck(player),
              () -> reward(player),
              () -> cancelSwimTask(player));
      swimTasks.put(player.getUniqueId(), repeatCheckTask.start(REQUIRED_SECONDS.get(), 20));

    } else {
      cancelSwimTask(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    cancelSwimTask(event.getPlayer().getBukkit());
  }

  private void cancelSwimTask(Player player) {
    BukkitTask task = swimTasks.remove(player.getUniqueId());
    if (task != null) {
      task.cancel();
    }
  }

  private boolean passesVibeCheck(Player player) {
    return LocationUtils.stoodInMaterial(player.getLocation(), Material.STATIONARY_LAVA);
  }
}
