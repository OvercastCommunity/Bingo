package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("numberjack")
public class NumberjackObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_COUNT = useConfig("required-count", 1000);

  private final Pattern pattern = Pattern.compile("\\d");

  // Get the largest number found max of 9
  // Add this to the current count
  // If the current count is equal to or greater than the required count then reward the player

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer victim = event.getVictim();
    if (victim == null) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    // When you kill a player regex out the numbers single digits in their name
    // TODO: get the name that the killer can see (check alphabet killer)
    Matcher matcher = pattern.matcher(victim.getNameLegacy());

    int largestDigit = 0;
    while (matcher.find()) {
      largestDigit = Math.max(Integer.parseInt(matcher.group()), largestDigit);
    }

    if (largestDigit == 0) return;

    trackProgress(killer.getBukkit(), largestDigit);
  }

  @Override
  protected int maxValue() {
    return REQUIRED_COUNT.get();
  }
}
