package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;

@Tracker("low-health-time")
public class LowHealthTimeObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_MINS = useConfig("required-mins", 15);
  private final Supplier<Integer> MAX_HEALTH = useConfig("max-health", 4);

  private final Map<UUID, Long> timerStartedAt = useState(Scope.LIFE);
  private final Map<UUID, BukkitTask> timerTasks = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onParticipantStop(ParticipantDespawnEvent event) {
    resetTimer(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;

    // Only allow one running timer per player
    if (timerStartedAt.containsKey(player.getUniqueId())) return;

    // Only allow when player will be below required health
    double finalHealth = player.getHealth() - event.getFinalDamage();
    if (finalHealth <= 0 || finalHealth > MAX_HEALTH.get()) return;

    startTimer(player);
    BukkitTask task =
        new BukkitRunnable() {
          int i = 0;

          @Override
          public void run() {
            if (!passesVibeCheck(player)) {
              resetTimer(player);
            } else if (++i % 10 == 0) {
              // Periodically update score
              refreshTimer(player);
            }
          }
        }.runTaskTimer(Bingo.get(), 20, 20);

    timerTasks.put(player.getUniqueId(), task);
  }

  public void startTimer(Player player) {
    resetTimer(player);
    timerStartedAt.put(player.getUniqueId(), System.currentTimeMillis());
  }

  public void resetTimer(Player player) {
    BukkitTask remove = timerTasks.remove(player.getUniqueId());
    if (remove != null) remove.cancel();

    calculateDuration(player);
  }

  public void refreshTimer(Player player) {
    calculateDuration(player);
    timerStartedAt.put(player.getUniqueId(), System.currentTimeMillis());
  }

  public void calculateDuration(Player player) {
    Long startedAt = timerStartedAt.remove(player.getUniqueId());
    if (startedAt == null) return;

    long secondsInState = (System.currentTimeMillis() - startedAt) / 1000;
    trackProgress(player, (int) secondsInState);
  }

  private boolean passesVibeCheck(Player player) {
    return (player.getMaxHealth() >= 10 && player.getHealth() <= MAX_HEALTH.get());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_MINS.get() * 60;
  }
}
