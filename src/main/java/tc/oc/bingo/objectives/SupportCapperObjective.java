package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@Tracker("support-capper")
public class SupportCapperObjective extends ObjectiveTracker {

  private final Supplier<Double> DISTANCE_NEARBY_PLAYERS = useConfig("nearby-player-distance", 6d);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWoolCapture(PlayerWoolPlaceEvent event) {
    ParticipantState player = event.getPlayer();
    List<Player> nearbyPlayers = getNearbyPlayers(player.getLocation());
    nearbyPlayers.removeIf(
        p -> p.getUniqueId().equals(player.getId()) || getParty(p) != player.getParty());
    if (!nearbyPlayers.isEmpty()) reward(nearbyPlayers);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagCapture(FlagCaptureEvent event) {
    MatchPlayer player = event.getCarrier();
    List<Player> nearbyPlayers = getNearbyPlayers(player.getLocation());
    nearbyPlayers.removeIf(p -> p.getUniqueId().equals(player.getId()));
    if (!nearbyPlayers.isEmpty()) reward(nearbyPlayers);
  }

  private List<Player> getNearbyPlayers(Location location) {
    return new ArrayList<>(location.getNearbyPlayers(DISTANCE_NEARBY_PLAYERS.get()));
  }
}
