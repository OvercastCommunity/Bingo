package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.BlockInfo;

@Tracker("anvil-killer")
public class AnvilKillerObjective extends ObjectiveTracker {

  private final Supplier<Boolean> REQUIRES_KILL = useConfig("requires-kill", false);

  private TrackerMatchModule tracker;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageByEntityEvent event) {
    if (REQUIRES_KILL.get() || tracker == null) return;

    if (!(event.getEntity() instanceof Player)) return;

    if (!event.getCause().equals(EntityDamageEvent.DamageCause.FALLING_BLOCK)) return;

    Entity actor = event.getActor();
    if (!(actor instanceof FallingBlock)) return;
    FallingBlock fallingBlock = (FallingBlock) actor;

    if (!fallingBlock.getMaterial().equals(Material.ANVIL)) return;

    ParticipantState owner = tracker.getEntityTracker().getOwner(event.getDamager());
    if (owner == null) return;

    MatchPlayer matchPlayer = owner.getPlayer().orElse(null);
    if (matchPlayer == null) return;

    reward(matchPlayer.getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (event.getDamageInfo() instanceof BlockInfo) {
      final Material material = ((BlockInfo) event.getDamageInfo()).getMaterial().getItemType();
      if (material == Material.ANVIL) {
        reward(player.getBukkit());
      }
    }
  }
}
