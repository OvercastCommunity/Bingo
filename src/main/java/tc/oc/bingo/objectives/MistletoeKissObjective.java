package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

@Tracker("mistletoe-kiss")
public class MistletoeKissObjective extends ObjectiveTracker {

  private final Supplier<Double> DETECTION_RADIUS = useConfig("max-distance", 0.5);
  private final Supplier<Double> DOT_PRODUCT_MIN = useConfig("min-dot-product", 0.85);

  private final Supplier<Set<Material>> BLOCKS_ALLOWED =
      useConfig("blocks-allowed", Set.of(Material.LEAVES, Material.LEAVES_2), MATERIAL_SET_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();

    // Check if the player is crouching (sneaking)
    if (!event.isSneaking()) return;

    // Check if there’s a block above that is a type of leaf
    if (!isUnderLeafBlock(player)) return;

    List<Player> rewardingPlayers = new ArrayList<>();

    // Look for nearby players within the detection radius
    for (Player nearbyPlayer : player.getLocation().getNearbyPlayers(DETECTION_RADIUS.get())) {
      if (nearbyPlayer == player) continue;

      // Check if the nearby player is sneaking and under a leaf block
      if (!nearbyPlayer.isSneaking() || !isUnderLeafBlock(nearbyPlayer)) continue;

      if (!arePlayersFacingEachOther(player, nearbyPlayer)) continue;

      // Check if the players are facing each other
      rewardingPlayers.add(nearbyPlayer);
    }

    // Add the event trigger if any other players match
    if (!rewardingPlayers.isEmpty()) rewardingPlayers.add(player);

    reward(rewardingPlayers);
  }

  private boolean isUnderLeafBlock(Player player) {
    Material type = player.getLocation().getBlock().getRelative(0, 2, 0).getType();
    return BLOCKS_ALLOWED.get().contains(type);
  }

  private boolean arePlayersFacingEachOther(Player player, Player friend) {
    // Get the unit direction vectors representing where each player is facing
    Vector playerFacing = player.getEyeLocation().getDirection();
    Vector friendFacing = friend.getEyeLocation().getDirection();

    // Calculate the vector from player to friend and normalize it
    Vector toPlayer2 =
        friend.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();

    // Check if player is looking at friend, and vice versa, using the dot product
    double dotProduct1 = playerFacing.dot(toPlayer2);
    double dotProduct2 = friendFacing.dot(toPlayer2.multiply(-1));

    return dotProduct1 >= DOT_PRODUCT_MIN.get() && dotProduct2 >= DOT_PRODUCT_MIN.get();
  }
}
