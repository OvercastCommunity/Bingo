package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.GrassSpecies;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.material.LongGrass;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

@Tracker("touch-grass")
public class TouchGrassObjective extends ObjectiveTracker.Stateful<Long> {

  private final Supplier<Double> GRASS_CHANCE = useConfig("grass-chance", 0.001d);

  private final Supplier<Double> MIN_BREAK_HOURS = useConfig("min-hour-break", 3d);
  private final Supplier<Double> MAX_BREAK_HOURS = useConfig("max-hour-break", 7d);

  private final Supplier<Long> MIN_BREAK_MILLIS =
      useComputedConfig(() -> (long) (MIN_BREAK_HOURS.get() * 1000L * 60L * 60L));

  private final Supplier<Long> MAX_BREAK_MILLIS =
      useComputedConfig(() -> (long) (MAX_BREAK_HOURS.get() * 1000L * 60L * 60L));

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onPlayerJoin(PlayerJoinMatchEvent event) {
    // When a player logs check the distance between the current time and the last played time
    // If the last played time is between min-hour-break and max-hour-break then reward them after a
    // 5 seconds delay
    event
        .getMatch()
        .getExecutor(MatchScope.LOADED)
        .schedule(
            () -> {
              MatchPlayer matchPlayer = event.getPlayer();
              Player player = matchPlayer.getBukkit();

              if (player == null || !player.isOnline()) return;

              long lastPlayed = getObjectiveData(player.getUniqueId());
              long timePassed = System.currentTimeMillis() - lastPlayed;
              if (timePassed > MIN_BREAK_MILLIS.get() && timePassed < MAX_BREAK_MILLIS.get()) {
                reward(player);
              }
            },
            5,
            TimeUnit.SECONDS);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public final void onMatchEnd(MatchFinishEvent event) {
    // When a match ends update the last played time for all players
    Collection<MatchPlayer> players = event.getMatch().getParticipants();
    players.forEach(player -> storeObjectiveData(player.getId(), System.currentTimeMillis()));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public final void onPlayerQuit(PlayerQuitEvent event) {
    // When a player logs out update the last played time
    Player player = event.getPlayer();
    if (player == null) return;
    storeObjectiveData(player.getUniqueId(), System.currentTimeMillis());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onGrassBreak(BlockBreakEvent event) {
    // When a player breaks a grass block (tall or regular) there's a random chance
    // GRASS_CHANCE.get() to complete the objective
    Player player = event.getPlayer();
    if (player == null) return;
    Material blockType = event.getBlock().getType();
    if (blockType != Material.GRASS
        && blockType != Material.LONG_GRASS
        && blockType != Material.DOUBLE_PLANT) return;
    if (blockType == Material.LONG_GRASS) {
      LongGrass data = (LongGrass) event.getBlock().getState().getData();
      if (data.getSpecies() != GrassSpecies.NORMAL) return;
    }
    if (blockType == Material.DOUBLE_PLANT) {
      if (event.getBlock().getData() != 2) return;
    }
    if (Math.random() < GRASS_CHANCE.get()) reward(player);
  }

  @Override
  public @NotNull Long initial() {
    return 0L;
  }

  @Override
  public @NotNull Long deserialize(@NotNull String string) {
    return Long.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Long data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Long data) {
    return 0;
  }

  @Override
  public Double getProgress(UUID uuid) {
    return null;
  }
}
