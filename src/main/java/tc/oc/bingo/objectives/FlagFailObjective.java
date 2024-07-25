package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagMatchModule;
import tc.oc.pgm.flag.event.FlagPickupEvent;

@Tracker("flag-fail")
public class FlagFailObjective extends ObjectiveTracker {

  private final Supplier<Integer> RADIUS = useConfig("radius", 3);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagPickup(FlagPickupEvent event) {
    Location flagLoc = event.getLocation();

    Match match = getMatch(flagLoc.getWorld());
    if (match == null) return;

    Collection<Player> nearbyPlayers = flagLoc.getNearbyPlayers(RADIUS.get());

    Collection<Player> rewardPlayers =
        nearbyPlayers.stream()
            .map(match::getPlayer)
            .filter(
                mp -> mp != null && mp.canInteract() && mp.isAlive() && mp != event.getCarrier())
            .map(MatchPlayer::getBukkit)
            .filter(
                p -> inPickupRange(p, flagLoc) && canPickup(match.getPlayer(p), event.getFlag()))
            .collect(Collectors.toList());

    reward(rewardPlayers);
  }

  // from pgm.flag.state.Uncarried#inPickupRange
  private boolean inPickupRange(Player player, Location flagLoc) {
    Location playerLoc = player.getLocation();

    if (playerLoc.getY() < flagLoc.getY() + 2
        && (playerLoc.getY() >= flagLoc.getY() - (player.isOnGround() ? 1 : 0.7))) {
      double dx = playerLoc.getX() - flagLoc.getX();
      double dz = playerLoc.getZ() - flagLoc.getZ();

      return dx * dx + dz * dz <= 1;
    }

    return false;
  }

  // from pgm.flag.state.Uncarried#canPickup
  private boolean canPickup(MatchPlayer player, Flag flag) {

    for (Flag flags : flag.getMatch().getModule(FlagMatchModule.class).getFlags()) {
      if (flags.isCarrying(player)) return false;
    }

    return flag.canPickup(player);
  }
}
