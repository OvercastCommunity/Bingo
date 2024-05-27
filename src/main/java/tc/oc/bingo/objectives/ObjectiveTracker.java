package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Data;
import lombok.Getter;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.ConfigHandler;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.bingo.util.PGMUtils;
import tc.oc.bingo.util.StateHandler;

@Data
@Log
public class ObjectiveTracker implements ManagedListener, ConfigHandler.Extensions, PGMUtils {

  private final String objectiveSlug;
  private final StateHandler state = new StateHandler();
  private final ConfigHandler config = new ConfigHandler();

  public ObjectiveTracker() {
    this.objectiveSlug = getClass().getDeclaredAnnotation(Tracker.class).value();
  }

  @Override
  public Stream<ManagedListener> children() {
    return Stream.of(state);
  }

  public final void setConfig(ConfigurationSection config) {
    this.config.reload(config);
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
    private final Map<UUID, T> progress = useState(Scope.SESSION);

    public abstract @NotNull T initial();

    public abstract @NotNull T deserialize(@NotNull String string);

    public abstract @NotNull String serialize(@NotNull T data);

    public abstract double progress(T data);

    @Override
    public Double getProgress(UUID uuid) {
      T state = progress.get(uuid);
      return state == null ? null : progress(state);
    }

    public void storeObjectiveData(UUID playerId, T data) {
      if (!progress.containsKey(playerId)) {
        log.warning(
            "Ignoring progress for " + playerId + " since it could lead to loss of progress");
        return;
      }
      progress.put(playerId, data);
      Bingo.get().getRewards().storeObjectiveData(playerId, getObjectiveSlug(), serialize(data));
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
  }
}
