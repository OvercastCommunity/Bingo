package tc.oc.bingo.objectives;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("item-drop")
public class ItemDropObjective extends ObjectiveTracker {

  private final Supplier<Material> THROWN_MATERIAL =
      useConfig("thrown-material", Material.WATER_BUCKET);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    Material stack = event.getItemDrop().getItemStack().getType();
    if (!stack.equals(THROWN_MATERIAL.get())) return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    PGM.get().getExecutor().schedule(() -> reward(event.getPlayer()), 2L, TimeUnit.SECONDS);
  }
}
