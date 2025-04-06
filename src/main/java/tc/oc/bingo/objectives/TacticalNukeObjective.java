package tc.oc.bingo.objectives;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;

@Tracker("tactical-nuke")
public class TacticalNukeObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 25);
  private final Supplier<Boolean> RESET_ON_CYCLE = useConfig("reset-on-cycle", true);

  private final Supplier<Integer> NUKE_TIMER = useConfig("nuke-timer-seconds", 10);

  private MatchPlayer nuker;

  private ScheduledFuture<?> nukeTask;
  private ScheduledFuture<?> lightningTask;

  private int timer;

  // Be in a match where someone gets a tactical nuke

  // A "tactical" nuke requires 25 kills without a death (kill-streak) don't actually use killstreak
  // stat due to below

  // Have a boolean config option for if this should be reset on participation end default to true
  // (goal can be made easier later)

  // Maybe reward the player with a rank "group.tacticalnuke" this could have a flair?

  // Reward all players in the match at that point

  // Do a short countdown sound effect type thing but don't actually do anything nuke related
  // WARNING PLAYER NAME CALLED IN A TACTICAL NUKE STRIKE INCOMING etc

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onDeath(MatchPlayerDeathEvent event) {
    MatchPlayer victim = event.getVictim();
    if (victim == null) return;

    resetObjectiveData(victim.getId());
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    // == and not >= so it only triggers once
    if (updateObjectiveData(killer.getId(), cur -> cur + 1).equals(KILLS_REQUIRED.get())) {
      triggerNuke(killer);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;
    resetObjectiveData(player.getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onParticipationStop(PlayerParticipationStopEvent event) {
    if (!RESET_ON_CYCLE.get()) return;
    resetObjectiveData(event.getPlayer().getId());
  }

  private void triggerNuke(MatchPlayer player) {
    if (nuker != null) return;
    nuker = player;
    timer = NUKE_TIMER.get();

    nukeTask =
        PGM.get().getExecutor().scheduleAtFixedRate(this::nukeCountdown, 0, 1, TimeUnit.SECONDS);
  }

  private void nukeCountdown() {
    if (nuker == null) return;
    final Match match = getMatch(nuker.getWorld());
    if (match == null) return;

    if (timer <= 0) {
      Collection<MatchPlayer> players = match.getParticipants();
      reward(players.stream().map(MatchPlayer::getBukkit).toList());

      // Reduce strike players down to limit at random
      List<Player> strikePlayers =
          players.stream().map(MatchPlayer::getBukkit).collect(Collectors.toList());

      Collections.shuffle(strikePlayers);
      strikePlayers = strikePlayers.stream().limit(15).collect(Collectors.toList());

      List<Player> finalStrikePlayers = strikePlayers;
      lightningTask =
          PGM.get()
              .getExecutor()
              .scheduleAtFixedRate(
                  () -> {
                    if (finalStrikePlayers.isEmpty()) {
                      lightningTask.cancel(true);
                      lightningTask = null;
                      return;
                    }
                    Player player = finalStrikePlayers.getFirst();
                    if (player.isOnline()) {
                      player.getWorld().strikeLightningEffect(player.getLocation());
                    }
                    finalStrikePlayers.remove(player);
                  },
                  0,
                  100,
                  TimeUnit.MILLISECONDS);

      nukeTask.cancel(true);
      nukeTask = null;
      nuker = null;
      return;
    }

    // 10 9 8 7 = GREEN
    // 6 5 4 = YELLOW
    // 3 2 1 = RED
    TextColor timeColor = NamedTextColor.RED;
    if (timer >= 7) timeColor = NamedTextColor.GREEN;
    else if (timer >= 4) timeColor = NamedTextColor.YELLOW;

    final Component mainTitle =
        Component.text("⚠ ☢ ", NamedTextColor.GOLD)
            .append(Component.text(timer, timeColor).decorate(TextDecoration.BOLD))
            .append(Component.text(" ☢ ⚠", NamedTextColor.GOLD));
    final Component subTitle =
        Component.text("TACTICAL NUKE LAUNCHED BY ", NamedTextColor.RED)
            .append(nuker.getName())
            .decorate(TextDecoration.BOLD);

    match.showTitle(
        Title.title(
            mainTitle,
            subTitle,
            Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(250))));
    match.playSound(Sound.sound(Key.key("mob.guardian.curse"), Sound.Source.BLOCK, 1f, 1f));
    timer--;
  }

  @Override
  protected int maxValue() {
    return KILLS_REQUIRED.get();
  }
}
