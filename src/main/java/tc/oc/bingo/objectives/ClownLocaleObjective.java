package tc.oc.bingo.objectives;

import java.util.Locale;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("clown-locale")
public class ClownLocaleObjective extends ObjectiveTracker {

  private final Set<Locale> clownLocales =
      Set.of(Locale.of("en", "UD"), Locale.of("en", "PT"), Locale.of("lol", "US"));

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    Locale killerLocale = killer.getBukkit().getLocale();

    if (clownLocales.contains(killerLocale)) {
      reward(killer.getBukkit());
    }
  }
}
