package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.killreward.KillRewardMatchModule;

@Tracker("kill-streak")
public class KillStreakObjective extends ObjectiveTracker {

  public static final int REQUIRED_STREAK = 10;

  public KillStreakObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    KillRewardMatchModule mm = event.getMatch().getModule(KillRewardMatchModule.class);
    if (mm == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    int streak = mm.getKillStreak(killer.getId());
    if (streak >= REQUIRED_STREAK) {
      reward(player.getBukkit());
    }
  }
}
