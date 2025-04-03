package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.material.TrapDoor;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("knock-knock")
public class KnockKnockObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_KNOCKS = useConfig("required-knocks", 1000);

  private final Map<UUID, Boolean> lastDoorClicked = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(PlayerInteractEvent event) { // maybe this event
    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

    // Check if player opened or closed a door or trap door caused by player increment

    if (event.getClickedBlock().getType().name().contains("DOOR")) {

      boolean isOpen = isDoorOpen(event.getClickedBlock());
      System.out.println("Door opened? " + isOpen);

      Boolean lastClicked = lastDoorClicked.get(player.getId());
      if (lastClicked != null && lastClicked != isOpen) {
        trackProgress(player.getBukkit());
      }

      lastDoorClicked.put(player.getId(), isOpen);
    }
  }

  private boolean isDoorOpen(Block block) {
    MaterialData materialData = block.getState().getMaterialData();

    if (materialData instanceof Door door) {
      // Top doors do not report the state change
      if (door.isTopHalf()) {
        return isDoorOpen(block.getRelative(BlockFace.DOWN));
      }

      return door.isOpen();
    }

    if (materialData instanceof TrapDoor trapDoor) {
      return trapDoor.isOpen();
    }

    return false;
  }

  @Override
  protected int maxValue() {
    return REQUIRED_KNOCKS.get();
  }
}
