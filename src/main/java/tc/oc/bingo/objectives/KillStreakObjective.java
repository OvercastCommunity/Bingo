package tc.oc.bingo.objectives;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.killreward.KillRewardMatchModule;

@Tracker("kill-streak")
public class KillStreakObjective extends ObjectiveTracker {

  public int requiredStreak = 10;
  private KillRewardMatchModule killRewardModule = null;

  @Override
  public void setConfig(ConfigurationSection config) {
    requiredStreak = config.getInt("required-streak", 10);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    killRewardModule = event.getMatch().getModule(KillRewardMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (killRewardModule == null) return;

    if (!event.isChallengeKill()) return;

    MatchPlayer player = getStatePlayer(event.getKiller());
    if (player == null) return;

    int streak = killRewardModule.getKillStreak(player.getId());
    if (streak >= requiredStreak) {
      reward(player.getBukkit());
    }
  }
}
