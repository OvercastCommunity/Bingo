package tc.oc.bingo.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.bingo.objectives.Scope;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

public class StateHandler implements ManagedListener {
  private final Map<Scope, List<Map<UUID, ?>>> state = new EnumMap<>(Scope.class);

  public void registerState(Scope scope, Map<UUID, ?> data) {
    state.computeIfAbsent(scope, k -> new ArrayList<>()).add(data);
  }

  private void clearState(UUID uuid, Scope... scopes) {
    for (Scope scope : scopes) {
      state.getOrDefault(scope, Collections.emptyList()).forEach(m -> m.remove(uuid));
    }
  }

  private void clearState(Scope... scopes) {
    for (Scope scope : scopes) {
      state.getOrDefault(scope, Collections.emptyList()).forEach(Map::clear);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerSpawn(ParticipantSpawnEvent event) {
    clearState(event.getPlayer().getId(), Scope.LIFE);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerDespawn(ParticipantDespawnEvent event) {
    clearState(event.getPlayer().getId(), Scope.LIFE);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTeamChange(PlayerPartyChangeEvent event) {
    clearState(event.getPlayer().getId(), Scope.PARTICIPATION);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerQuit(PlayerQuitEvent event) {
    clearState(event.getPlayer().getUniqueId(), Scope.values());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerSpawn(MatchStartEvent event) {
    clearState(Scope.LIFE, Scope.PARTICIPATION, Scope.MATCH);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerSpawn(MatchFinishEvent event) {
    clearState(Scope.LIFE, Scope.PARTICIPATION, Scope.MATCH);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onMatchLoad(MatchAfterLoadEvent event) {
    clearState(Scope.LIFE, Scope.PARTICIPATION, Scope.MATCH, Scope.FULL_MATCH);
  }
}
