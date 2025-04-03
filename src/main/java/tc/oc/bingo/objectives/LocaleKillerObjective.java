package tc.oc.bingo.objectives;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("locale-killer")
public class LocaleKillerObjective extends ObjectiveTracker.Stateful<Set<String>> {

  private final Supplier<Integer> REQUIRED_LOCALES = useConfig("required-locales", 5);
  private final Supplier<Boolean> NON_SAME_LOCALE = useConfig("non-same-locale", true);

  // TODO: do an objective that requires pirate to start with? does 1.7 have this?

  // Locale.of("en", "UD")

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    MatchPlayer victim = event.getPlayer();
    String victimLocale =
        victim.getBukkit().getLocale().toLanguageTag(); // TODO: how to get stuff like pirate?

    // TODO: require different to own?
    if (NON_SAME_LOCALE.get()
        && victimLocale.equals(killer.getBukkit().getLocale().toLanguageTag())) return;

    UUID killerId = killer.getId();
    Set<String> locales =
        updateObjectiveData(
            killerId,
            strings -> {
              strings.add(victimLocale);
              return strings;
            });

    if (locales.size() >= REQUIRED_LOCALES.get()) {
      reward(killer.getBukkit());
    }
  }

  @Override
  public @NotNull Set<String> initial() {
    return new HashSet<>();
  }

  @Override
  public @NotNull Set<String> deserialize(@NotNull String string) {
    Set<String> locales = new HashSet<>();
    for (String locale : string.split(",")) {
      locales.add(locale.trim());
    }
    return locales;
  }

  @Override
  public @NotNull String serialize(@NotNull Set<String> data) {
    return String.join(",", data);
  }

  @Override
  public double progress(Set<String> data) {
    return (double) data.size() / REQUIRED_LOCALES.get();
  }
}
