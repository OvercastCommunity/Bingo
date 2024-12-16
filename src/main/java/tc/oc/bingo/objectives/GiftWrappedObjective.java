package tc.oc.bingo.objectives;

import java.util.Deque;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.damagehistory.DamageEntry;
import tc.oc.pgm.damagehistory.DamageHistoryMatchModule;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("gift-wrapped")
public class GiftWrappedObjective extends ObjectiveTracker {

  private final Supplier<Double> MAX_DAMAGE = useConfig("max-damage", 6d);

  private DamageHistoryMatchModule history;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    history = event.getMatch().needModule(DamageHistoryMatchModule.class);
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    if (!(event.getDamageInfo() instanceof MeleeInfo info)) return;
    if (!(info.getWeapon() instanceof ItemInfo)) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    MatchPlayer victim = event.getVictim();
    if (victim == null) return;

    Deque<DamageEntry> damageHistory = history.getDamageHistory(victim);

    // Killer was the only attacker
    if (damageHistory.size() <= 1) return;

    // Damage amount was too much
    DamageEntry last = damageHistory.getLast();
    if (last == null || last.getDamage() >= MAX_DAMAGE.get()) return;

    // Check the killer only occurs once
    long killerEntries =
        damageHistory.stream()
            .filter(
                entry -> entry.getDamager() != null && entry.getDamager().getId() == killer.getId())
            .count();

    if (killerEntries > 1) return;

    reward(killer.getBukkit());
  }
}
