package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.Getter;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.ConfigHandler;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.bingo.util.PGMUtils;
import tc.oc.bingo.util.StateHandler;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

@Data
@Log
public class ObjectiveTracker implements ManagedListener, ConfigHandler.Extensions, PGMUtils {

  private String objectiveSlug;
  private final StateHandler state = new StateHandler();
  private final ConfigHandler config = new ConfigHandler();

  public ObjectiveTracker() {
    // Objective slug is replaced by tracker slug on creation
    // This can be the same if no variant is used or a colon split value if a variant.
    this.objectiveSlug = getClass().getDeclaredAnnotation(Tracker.class).value();
  }

  @Override
  public Stream<ManagedListener> children() {
    return Stream.of(state);
  }

  public final void setConfig(ConfigurationSection config) {
    this.config.reload(config);
  }

  @Override
  public String getConfigSection() {
    return this.objectiveSlug;
  }

  protected final void reward(Collection<Player> players) {
    Bingo.get().getRewards().rewardPlayers(objectiveSlug, players);
  }

  protected final void reward(Player player) {
    reward(Collections.singletonList(player));
  }

  protected final <T> Map<UUID, T> useState(Scope scope) {
    Map<UUID, T> result = new HashMap<>();
    state.registerState(scope, result);
    return result;
  }

  public Double getProgress(UUID uuid) {
    return null;
  }

  @Getter
  public abstract static class Stateful<T> extends ObjectiveTracker {
    private final Map<UUID, T> progress = new HashMap<>();
    private final Set<UUID> dirtyProgress = new HashSet<>();

    public abstract @NotNull T initial();

    public abstract @NotNull T deserialize(@NotNull String string);

    public abstract @NotNull String serialize(@NotNull T data);

    public abstract double progress(T data);

    @Override
    public Double getProgress(UUID uuid) {
      T state = getObjectiveData(uuid);
      return state == null ? null : progress(state);
    }

    public T getObjectiveData(UUID playerId) {
      T data =
          progress.computeIfAbsent(
              playerId,
              uuid -> {
                BingoPlayerCard bingoPlayerCard = Bingo.get().getCards().get(playerId);
                // This is actual trouble. We need a players' data, but it hasn't been loaded yet.
                // We not just need to make up some data, but make sure it doesn't stay when the
                // real one comes in.
                if (bingoPlayerCard == null) {
                  log.warning(
                      "Card data for " + playerId + " hasn't loaded for " + getObjectiveSlug());
                  return null;
                }

                ProgressItem pi = bingoPlayerCard.getProgress(getObjectiveSlug());
                if (pi.getData() == null) return initial();
                return deserialize(pi.getData());
              });

      // If player card failed loading, avoid saving this as valid.
      if (data == null) {
        progress.remove(playerId);
        return initial();
      }

      return data;
    }

    public void storeObjectiveData(UUID playerId, T data) {
      if (!progress.containsKey(playerId)) {
        // This can happen due to one of two things:
        // a) no prior call to getObjectiveData. In which case, consider using resetObjectiveData
        // b) bingo data hasn't loaded yet; we must avoid saving as it could lose progress
        log.warning(
            "Ignoring progress for " + playerId + " since it could lead to loss of progress");
        return;
      }
      progress.put(playerId, data);
      dirtyProgress.add(playerId);
    }

    public void resetObjectiveData(UUID playerId) {
      progress.put(playerId, initial());
      dirtyProgress.add(playerId);
    }

    public T updateObjectiveData(UUID playerId, Function<T, T> updater) {
      T objectiveData = updater.apply(getObjectiveData(playerId));
      storeObjectiveData(playerId, objectiveData);
      return objectiveData;
    }

    private void persistObjectiveData(UUID playerId, boolean remove) {
      T data = remove ? progress.remove(playerId) : progress.get(playerId);
      if (!dirtyProgress.remove(playerId) || data == null) return;

      Bingo.get().getRewards().storeObjectiveData(playerId, getObjectiveSlug(), serialize(data));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent event) {
      persistObjectiveData(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchFinish(MatchFinishEvent event) {
      event
          .getMatch()
          .getPlayers()
          .forEach(matchPlayer -> persistObjectiveData(matchPlayer.getId(), false));
    }
  }

  public abstract static class StatefulInt extends Stateful<Integer> {

    protected abstract int maxValue();

    protected void trackProgress(Player player) {
      trackProgress(player, 1);
    }

    protected void trackProgress(Player player, int amount) {
      if (player == null) return;
      Integer interactions = updateObjectiveData(player.getUniqueId(), curr -> curr + amount);

      // Check if the player has completed the objective
      if (interactions >= maxValue()) {
        reward(player);
      }
    }

    protected void trackProgress(Collection<? extends Player> players) {
      trackProgress(players, p -> 1);
    }

    protected void trackProgress(
        Collection<? extends Player> players, Function<Player, Integer> update) {
      if (players == null) return;
      List<Player> toReward = new ArrayList<>(players.size());
      for (Player player : players) {
        if (player == null) continue;
        Integer interactions =
            updateObjectiveData(player.getUniqueId(), curr -> curr + update.apply(player));
        if (interactions >= maxValue()) toReward.add(player);
      }
      reward(toReward);
    }

    @Override
    public @NotNull Integer initial() {
      return 0;
    }

    @Override
    public @NotNull Integer deserialize(@NotNull String string) {
      return Integer.valueOf(string);
    }

    @Override
    public @NotNull String serialize(@NotNull Integer data) {
      return String.valueOf(data);
    }

    @Override
    public double progress(Integer data) {
      return (double) data / maxValue();
    }
  }

  public abstract static class StatefulSet<T> extends Stateful<Set<T>> {

    protected abstract int maxCount();

    protected void trackProgress(Player player, T value) {
      if (player == null || value == null) return;

      Set<T> updated =
          updateObjectiveData(
              player.getUniqueId(),
              current -> {
                current.add(value);
                return current;
              });

      if (updated.size() >= maxCount()) {
        reward(player);
      }
    }

    @Override
    public @NotNull Set<T> initial() {
      return new HashSet<>();
    }

    @Override
    public @NotNull Set<T> deserialize(@NotNull String string) {
      if (string.isEmpty()) return initial();
      return Arrays.stream(string.split(","))
          .map(this::deserializeElement)
          .collect(Collectors.toSet());
    }

    @Override
    public @NotNull String serialize(@NotNull Set<T> data) {
      return data.stream().map(this::serializeElement).collect(Collectors.joining(","));
    }

    @Override
    public double progress(Set<T> data) {
      return (double) data.size() / maxCount();
    }

    // Should be overridden by subclasses to handle specific types
    protected T deserializeElement(String string) {
      @SuppressWarnings("unchecked")
      T value = (T) string;
      return value;
    }

    protected String serializeElement(T value) {
      return value.toString();
    }
  }
}
