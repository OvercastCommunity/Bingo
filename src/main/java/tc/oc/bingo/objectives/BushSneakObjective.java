package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import tc.oc.bingo.config.ConfigReader;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("bush-sneak")
public class BushSneakObjective extends ObjectiveTracker {

  private static final ConfigReader<Material> MATERIAL_NAME_READER =
      (cfg, key, def) -> Material.getMaterial(cfg.getString(key));

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("material-name", Material.DOUBLE_PLANT, MATERIAL_NAME_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if (!event.isSneaking()) return;

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null) return;

    // TODO: check if match player? or allow observers?
    if (!matchPlayer.getMatch().isRunning() || !matchPlayer.isParticipating()) return;

    if (LocationUtils.stoodInMaterial(matchPlayer.getLocation(), MATERIAL_REQUIRED.get())) {
      reward(event.getPlayer());
    }
  }
}
