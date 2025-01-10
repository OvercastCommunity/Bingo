package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("armor-stand-dresser")
public class ArmorStandDresserObjective extends ObjectiveTracker {

  private UUID placer = null; // Tracks placed armour stands
  private final Map<UUID, Integer> placedArmourStands = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmourStandPlace(PlayerInteractEvent event) {
    // Triggered when a player places an armor stand
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getItem() == null || event.getItem().getType() != Material.ARMOR_STAND) return;

    placer = event.getPlayer().getUniqueId();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmorStandSpawn(EntitySpawnEvent event) {
    if (placer == null) return;

    UUID placer = this.placer;
    this.placer = null;

    placedArmourStands.put(placer, event.getEntity().getEntityId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmorStandChange(PlayerArmorStandManipulateEvent event) {
    Player player = event.getPlayer();
    ArmorStand rightClicked = event.getRightClicked();

    // Check this was the one placed by player
    Integer armorStandId = placedArmourStands.get(player.getUniqueId());
    if (armorStandId == null || armorStandId != rightClicked.getEntityId()) return;

    ItemStack playerItem = event.getPlayerItem();

    if (isArmorPiece(playerItem)) reward(player);
  }

  public static boolean isArmorPiece(ItemStack itemStack) {
    if (itemStack == null || itemStack.getType() == Material.AIR) return false;

    return switch (itemStack.getType()) {
      case LEATHER_HELMET,
              LEATHER_CHESTPLATE,
              LEATHER_LEGGINGS,
              LEATHER_BOOTS,
              IRON_HELMET,
              IRON_CHESTPLATE,
              IRON_LEGGINGS,
              IRON_BOOTS,
              GOLD_HELMET,
              GOLD_CHESTPLATE,
              GOLD_LEGGINGS,
              GOLD_BOOTS,
              DIAMOND_HELMET,
              DIAMOND_CHESTPLATE,
              DIAMOND_LEGGINGS,
              DIAMOND_BOOTS ->
          true;
      default -> false;
    };
  }
}
