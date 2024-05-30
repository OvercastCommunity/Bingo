package tc.oc.bingo.objectives;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.death.DeathMessageBuilder;
import tc.oc.pgm.util.text.TextTranslations;

@Log
@Tracker("kill-messages")
public class KillTypeObjective extends ObjectiveTracker.Stateful<Set<Integer>> {

  private static final Map<String, Integer> DEATH_MESSAGE_KEYS = new HashMap<>();

  static {
    int i = 0;
    for (String s : TextTranslations.getKeys().tailSet("death.")) {
      DEATH_MESSAGE_KEYS.put(s, i++);
    }
  }

  private final Supplier<Integer> MIN_MESSAGE_COUNT = useConfig("min-death-message-types", 20);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    MatchPlayer victim = event.getPlayer();
    MatchPlayer killer = getStatePlayer(event.getKiller());

    Component message = new DeathMessageBuilder(event, log).getMessage();
    if (!(message instanceof TranslatableComponent)) return;

    TranslatableComponent translatableComponent = (TranslatableComponent) message;
    Integer deathMessageIndex = DEATH_MESSAGE_KEYS.get(translatableComponent.key());
    if (deathMessageIndex == null) return;

    List<Player> rewardingPlayers = new ArrayList<>(2);
    if (storeMessageIndex(killer, deathMessageIndex)) rewardingPlayers.add(killer.getBukkit());
    if (storeMessageIndex(victim, deathMessageIndex)) rewardingPlayers.add(victim.getBukkit());

    reward(rewardingPlayers);
  }

  private boolean storeMessageIndex(MatchPlayer player, Integer deathMessageIndex) {
    if (player == null) return false;

    return updateObjectiveData(
                player.getId(),
                idx -> {
                  idx.add(deathMessageIndex);
                  return idx;
                })
            .size()
        >= MIN_MESSAGE_COUNT.get();
  }

  @Override
  public @NotNull Set<Integer> initial() {
    return new HashSet<>();
  }

  @Override
  public @NotNull Set<Integer> deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Arrays.stream(string.split(",")).map(Integer::valueOf).collect(Collectors.toSet());
  }

  @Override
  public @NotNull String serialize(@NotNull Set<Integer> data) {
    return String.join(",", Iterables.transform(data, Object::toString));
  }

  @Override
  public double progress(Set<Integer> data) {
    return (double) data.size() / MIN_MESSAGE_COUNT.get();
  }
}
