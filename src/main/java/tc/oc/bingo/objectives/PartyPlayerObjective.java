package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("party-player")
public class PartyPlayerObjective extends ObjectiveTracker {

  private final Supplier<Integer> PARTY_SIZE_REQUIRED = useConfig("party-size-required", 3);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onMatchStart(MatchStartEvent event) {
    List<UUID> rewardingUUIDs = new ArrayList<>();

    event
        .getMatch()
        .getParticipants()
        .forEach(
            player -> {
              if (rewardingUUIDs.contains(player.getId())) return;

              // Retrieve the player's party using PGM integration
              Collection<UUID> squad = Integration.getSquad(player.getBukkit());

              if (squad != null && squad.size() >= PARTY_SIZE_REQUIRED.get()) {
                rewardingUUIDs.addAll(squad);
              }
            });

    List<Player> rewardingPlayers =
        rewardingUUIDs.stream()
            .map(
                uuid -> {
                  MatchPlayer player = event.getMatch().getPlayer(uuid);
                  if (player == null) return null;
                  return player.getBukkit();
                })
            .filter(Objects::nonNull)
            .toList();

    reward(rewardingPlayers);
  }
}
