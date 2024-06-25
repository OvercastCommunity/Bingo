package tc.oc.bingo.objectives;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@Tracker("golden-apple-full")
public class GappleOverhealObjective extends ObjectiveTracker {

  private final Supplier<Double> MIN_ABSORPTION_HEALTH = useConfig("min-absorption-health", 4.0);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Material item = event.getItem().getType();
    if (item != Material.GOLDEN_APPLE) return;

    Player player = event.getPlayer();

    double health = player.getHealth();
    double absorption = PLAYER_UTILS.getAbsorption(player);

    if (health >= player.getMaxHealth() && absorption >= MIN_ABSORPTION_HEALTH.get()) {
      reward(player);
    }
  }
}
