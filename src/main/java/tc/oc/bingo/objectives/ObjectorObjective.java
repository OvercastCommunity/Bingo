package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;

@Tracker("objector-objective")
public class ObjectorObjective extends ObjectiveTracker {

  private static final Set<Material> SWORD_TYPES =
      EnumSet.of(
          Material.WOOD_SWORD,
          Material.STONE_SWORD,
          Material.IRON_SWORD,
          Material.GOLD_SWORD,
          Material.DIAMOND_SWORD);

  private final Map<UUID, Set<Material>> itemsDroppedByPlayer = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    Player player = event.getPlayer();
    UUID uniqueId = player.getUniqueId();
    Material itemType = event.getItemDrop().getItemStack().getType();

    if (SWORD_TYPES.contains(itemType)) {
      // If it's any kind of sword, treat it as a wooden sword
      itemsDroppedByPlayer
          .computeIfAbsent(uniqueId, uuid -> new HashSet<>())
          .add(Material.WOOD_SWORD);

    } else if (itemType == Material.BOW) {
      itemsDroppedByPlayer.computeIfAbsent(uniqueId, uuid -> new HashSet<>()).add(Material.BOW);
    } else {
      return;
    }

    if (itemsDroppedByPlayer.get(uniqueId).size() >= 2) reward(player);
  }
}
