package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.trackers.EntityTracker;

@Tracker("hook-em")
public class HookEmObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_SECONDS = useConfig("max-seconds", 5);

  private EntityTracker tracker;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchAfterLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class).getEntityTracker();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;

    Bukkit.broadcastMessage("CAUGHT ENTITY");
    // TODO:
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(ProjectileHitEvent event) {
    // TODO:
    if (!(event.getEntity() instanceof FishHook fishHook)) return;
    if (!(event.getActor() instanceof Player player)) return;

    MatchPlayer hooker = getPlayer(tracker.getOwner(fishHook));
    if (hooker == null) return;

    Bukkit.broadcastMessage("PROJECTILE HIT from " + hooker.getNameLegacy());
  }
}
