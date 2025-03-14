package tc.oc.bingo.objectives;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;

@Tracker("tactical-nuke")
public class TacticalNukeObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 25);
  private final Supplier<Boolean> RESET_ON_CYCLE = useConfig("reset-on-cycle", true);

  private MatchPlayer nuker;
  private BukkitTask nukeTask;
  private final Map<Integer, BukkitTask> timerTasks = new HashMap<>();
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

    storeObjectiveData(victim.getId(), 0);
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    // == and not >= so it only triggers once
    if (updateObjectiveData(killer.getId(), cur -> cur + 1) == KILLS_REQUIRED.get()) {
      triggerNuke(killer);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;
    storeObjectiveData(player.getUniqueId(), 0);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onParticipationStop(PlayerParticipationStopEvent event) {
    if (!RESET_ON_CYCLE.get()) return;

    storeObjectiveData(event.getPlayer().getId(), 0);
  }

  private void triggerNuke(MatchPlayer player) {
    if (nuker != null) return;
    nuker = player;
    timer = 10;
    BukkitTask task =
        new BukkitRunnable() {
          @Override
          public void run() {
            nukeCountdown();
          }
        }.runTaskTimer(Bingo.get(), 20L, 20L);
    nukeTask = task;
  }

  private void nukeCountdown() {
    if (nuker == null) return;
    final Match match = getMatch(nuker.getWorld());
    if (match == null) return;
    if (timer <= 0) {
      timerTasks.forEach(
          (integer, bukkitTask) -> {
            bukkitTask.cancel();
          });
      timerTasks.clear();

      Collection<MatchPlayer> players = match.getParticipants();
      Iterator<MatchPlayer> iterator = players.iterator();
      reward(players.stream().map(MatchPlayer::getBukkit).toList());

      int index = 1;
      while (iterator.hasNext()) {
        MatchPlayer player = iterator.next();
        final int finalIndex = index;
        BukkitTask task =
            new BukkitRunnable() {
              @Override
              public void run() {
                player.getWorld().strikeLightningEffect(player.getLocation());
                timerTasks.remove(finalIndex);
              }
            }.runTaskLater(Bingo.get(), index);
        timerTasks.put(index, task);
        index++;
      }
      nukeTask.cancel();
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

    // TODO: name components?
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
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.parseInt(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return data / (double) KILLS_REQUIRED.get();
  }
}
