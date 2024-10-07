package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.ConfigReader;
import tc.oc.bingo.util.LocationUtils;

@Tracker("bush-sneak")
public class BushSneakObjective extends ObjectiveTracker {

  private static final ConfigReader<Material> MATERIAL_NAME_READER =
      (cfg, key, def) -> Material.getMaterial(cfg.getString(key));

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("material-name", Material.DOUBLE_PLANT, MATERIAL_NAME_READER);

  private final Supplier<Integer> REQUIRED_SECONDS = useConfig("required-seconds", 10);

  private final Map<UUID, BukkitTask> sneakTasks = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if (notParticipating(event.getPlayer())) return;

    if (!event.isSneaking()) {
      // Player stopped sneaking, cancel any scheduled task
      BukkitTask task = sneakTasks.remove(event.getPlayer().getUniqueId());
      if (task != null) task.cancel();
      return;
    }

    if (passesVibeCheck(event.getPlayer())) {
      BukkitTask task =
          new BukkitRunnable() {
            @Override
            public void run() {
              if (event.getPlayer().isSneaking() && passesVibeCheck(event.getPlayer())) {
                reward(event.getPlayer());
              }
              sneakTasks.remove(event.getPlayer().getUniqueId());
            }
          }.runTaskLater(Bingo.get(), REQUIRED_SECONDS.get() * 20);

      sneakTasks.put(event.getPlayer().getUniqueId(), task);
    }
  }

  private boolean passesVibeCheck(Player player) {
    return LocationUtils.stoodInMaterial(player.getLocation(), MATERIAL_REQUIRED.get());
  }
}
