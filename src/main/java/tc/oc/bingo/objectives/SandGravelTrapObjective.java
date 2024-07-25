package tc.oc.bingo.objectives;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.BlockInfo;

@Tracker("sand-gravel-trap")
public class SandGravelTrapObjective extends ObjectiveTracker {

  private TrackerMatchModule tracker;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDamageByBlock(EntityDamageEvent event) {
    if (Objects.isNull(tracker)) return;
    if (!(event.getEntity() instanceof Player)) return;
    if (!event.getCause().equals(EntityDamageEvent.DamageCause.SUFFOCATION)) return;

    BlockInfo blockInfo = (BlockInfo) tracker.resolveDamage(event);
    if (Objects.isNull(blockInfo)) return;
    Material material = blockInfo.getMaterial().getItemType();
    if (!material.equals(Material.SAND) && !material.equals(Material.GRAVEL)) return;

    ParticipantState owner = tracker.resolveDamage(event).getAttacker();
    if (Objects.isNull(owner)) return;
    MatchPlayer matchPlayer = owner.getPlayer().orElse(null);
    if (Objects.isNull(matchPlayer)) return;

    reward(matchPlayer.getBukkit());
  }
}
