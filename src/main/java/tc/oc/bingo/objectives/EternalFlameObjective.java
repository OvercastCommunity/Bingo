package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import tc.oc.bingo.util.RepeatCheckTask;
import tc.oc.pgm.api.match.Match;

@Tracker("eternal-flame")
public class EternalFlameObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_TIME = useConfig("required-time", 60);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    if (event.getItem() == null || event.getItem().getType() != Material.FLINT_AND_STEEL) return;
    Block block = event.getClickedBlock();

    if (block == null) return;
    Block targetBlock = block.getRelative(event.getBlockFace());
    if (!targetBlock.getType().equals(Material.AIR)) return;

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    (new RepeatCheckTask(() -> passesVibeCheck(match, targetBlock), () -> reward(player)))
        .start(REQUIRED_TIME.get());
  }

  private boolean passesVibeCheck(Match match, Block block) {
    if (!match.isRunning()) return false;

    return (block != null && block.getType() == Material.FIRE);
  }
}
