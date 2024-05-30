package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.bingo.util.InventoryUtil;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("berserker")
public class BerserkerObjective extends ObjectiveTracker {

  private final Set<Material> AXE_MATERIALS =
      EnumSet.of(
          Material.WOOD_AXE,
          Material.STONE_AXE,
          Material.GOLD_AXE,
          Material.IRON_AXE,
          Material.DIAMOND_AXE);
  private final Set<Material> SWORD_MATERIALS =
      EnumSet.of(
          Material.WOOD_SWORD,
          Material.STONE_SWORD,
          Material.GOLD_SWORD,
          Material.IRON_SWORD,
          Material.DIAMOND_SWORD);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (event.getDamageInfo() instanceof MeleeInfo) {
      MeleeInfo info = (MeleeInfo) event.getDamageInfo();
      if (info.getWeapon() instanceof ItemInfo) {
        ItemInfo weapon = (ItemInfo) info.getWeapon();
        if (weapon != null && AXE_MATERIALS.contains(weapon.getItem().getType())) {
          // Check if player has a sword in their inventory
          PlayerInventory inventory = player.getBukkit().getInventory();
          if (InventoryUtil.containsAny(inventory, it -> SWORD_MATERIALS.contains(it.getType()))) {
            reward(player.getBukkit());
          }
        }
      }
    }
  }
}
