package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.Cauldron;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.BukkitUtils;

@Tracker("cauldron-item")
public class CauldronItemObjective extends ObjectiveTracker {

  private final Color DEFAULT_LEATHER_COLOR = Bukkit.getItemFactory().getDefaultLeatherColor();

  private final Set<Material> LEATHER_ARMOR =
      EnumSet.of(
          Material.LEATHER_HELMET,
          Material.LEATHER_CHESTPLATE,
          Material.LEATHER_LEGGINGS,
          Material.LEATHER_BOOTS);

  private final Map<Color, DyeColor> ALLOWED_DYE_DROPS =
      Map.of(
          BukkitUtils.colorOf(ChatColor.BLUE), DyeColor.BLUE,
          BukkitUtils.colorOf(ChatColor.DARK_RED), DyeColor.RED);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getClickedBlock().getType() != Material.CAULDRON) return;

    Player player = event.getPlayer();
    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    ItemStack itemInHand = event.getItem();
    if (itemInHand == null) return;

    if (!LEATHER_ARMOR.contains(itemInHand.getType())) return;

    // Check if the armor is dyed
    if (!(itemInHand.getItemMeta() instanceof LeatherArmorMeta itemMeta)) return;
    if (itemMeta.getColor().equals(DEFAULT_LEATHER_COLOR)) return;

    // Check cauldron has water
    Block block = event.getClickedBlock();
    MaterialData materialData = block.getState().getMaterialData();

    if (!(materialData instanceof Cauldron cauldron)) return;
    if (cauldron.isEmpty()) return;

    // Remove the dye from the item (reset to default color)
    Color dyedColor = itemMeta.getColor();

    // Reward player
    reward(player);

    // Try to find a matching dye color for custom drops
    DyeColor dyeColor = ALLOWED_DYE_DROPS.get(dyedColor);
    if (dyeColor == null) return;

    // Create an ink sack (dye) with that color and spawn it naturally above the cauldron
    ItemStack item = new ItemStack(Material.INK_SACK, 1);
    item.setData(new Dye(dyeColor));
    item.setDurability(item.getData().getData());

    // Play effect and sound, then drop the item
    Location effectLocation = block.getLocation().add(0.5, 1, 0.5);
    block.getWorld().playEffect(effectLocation, Effect.EXPLOSION_LARGE, 1);
    block.getWorld().playSound(effectLocation, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f);
    block.getWorld().dropItemNaturally(effectLocation, item);
  }
}
