package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.util.RepeatCheckTask;

@Tracker("eternal-flame")
public class EternalFlameObjective extends ObjectiveTracker {

  //    private final Map<UUID, Long> flameStartTimes = useState(Scope.LIFE);

  private final Supplier<Integer> REQUIRED_TIME =
      useConfig("required-time", 10); // TODO: check vanilla and change

  private final Map<UUID, BukkitTask> flameTasks = new HashMap<>(); // To track tasks for players

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    if (event.getItem() == null || event.getItem().getType() != Material.FLINT_AND_STEEL) return;
    Block block = event.getClickedBlock();

    if (block == null) return;
    Block targetBlock = block.getRelative(event.getBlockFace());
    if (!targetBlock.getType().equals(Material.AIR)) return;

    RepeatCheckTask repeatCheckTask =
        new RepeatCheckTask(() -> passesVibeCheck(targetBlock), () -> reward(player));
    flameTasks.put(player.getUniqueId(), repeatCheckTask.start(REQUIRED_TIME.get()));

    //        long currentTime = System.currentTimeMillis();
    //        long timePassed = (currentTime - flameStartTimes.getOrDefault(player.getUniqueId(),
    // currentTime)) / 1000;

    //        if (timePassed >= REQUIRED_TIME.get()) {
    //            reward(player);
    //        }
  }

  private boolean passesVibeCheck(Block block) {
    return (block != null
        && block.getType()
            == Material.FIRE); // TODO: match end check in here somewhere or player offline
  }
}
