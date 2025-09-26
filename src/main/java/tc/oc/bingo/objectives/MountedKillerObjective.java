package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("mounted-killer")
public class MountedKillerObjective extends ObjectiveTracker {

  private final Supplier<EntityType> ENTITY_TYPE = useConfig("entity-type", EntityType.HORSE);

  private final Map<UUID, Set<UUID>> attackedPlayers = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager)) return;
    if (!(event.getEntity() instanceof Player damaged)) return;

    // Only track whilst mounted
    if (!passesVibeCheck(damager)) return;

    attackedPlayers
        .computeIfAbsent(damager.getUniqueId(), k -> new java.util.HashSet<>())
        .add(damaged.getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer matchPlayer = killer.getPlayer().orElse(null);
    if (matchPlayer == null) return;

    MatchPlayer victim = event.getVictim().getPlayer();
    if (victim == null) return;

    Player player = matchPlayer.getBukkit();

    // Require that the killer attacked the victim at some point whilst mounted
    if (!attackedPlayers.containsKey(player.getUniqueId())) return;
    if (!attackedPlayers.get(player.getUniqueId()).contains(victim.getId())) return;

    if (passesVibeCheck(player)) {
      reward(player);
    }
  }

  public boolean passesVibeCheck(Player player) {
    Entity vehicle = player.getVehicle();
    return (vehicle != null && vehicle.getType().equals(ENTITY_TYPE.get()));
  }
}
