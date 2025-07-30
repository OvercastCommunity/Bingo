package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.material.Cauldron;
import org.bukkit.material.MaterialData;
import tc.oc.bingo.util.RepeatCheckTask;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

@Tracker("cooling-off")
public class CoolingOffObjective extends ObjectiveTracker {

  private final Supplier<Integer> STANDING_SECONDS = useConfig("standing-seconds", 5);

  @EventHandler(ignoreCancelled = true)
  public void onPlayerCoarseMoveEvent(PlayerCoarseMoveEvent event) {
    if (passesVibeCheck(event.getPlayer())) return;

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    Player player = matchPlayer.getBukkit();
    new RepeatCheckTask(
            RepeatCheckTask.CheckMode.CONTINUOUS,
            () -> passesVibeCheck(player),
            () -> reward(player))
        .start(STANDING_SECONDS.get());
  }

  private boolean passesVibeCheck(Player player) {
    if (player == null || !player.isOnline()) return false;

    Block block = player.getLocation().getBlock();
    if (!block.getType().equals(Material.CAULDRON)) return false;

    MaterialData materialData = block.getState().getMaterialData();

    if (materialData instanceof Cauldron cauldron) {
      return !cauldron.isEmpty();
    }

    return false;
  }
}
