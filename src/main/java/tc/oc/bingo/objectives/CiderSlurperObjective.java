package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;
import tc.oc.bingo.util.RepeatCheckTask;
import tc.oc.pgm.util.inventory.tag.ItemTag;

@Tracker("cider-slurper")
public class CiderSlurperObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> APPLE_ITEM = CustomItem.of("apple");

  public static final ItemTag<Boolean> POTION_META = ItemTag.newBoolean("cider-slurper");

  private final Supplier<Integer> CONVERT_SECONDS = useConfig("convert-seconds", 10);

  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    Item itemDrop = event.getItemDrop();
    ItemStack itemStack = itemDrop.getItemStack();

    if (!CustomItemModule.isCustomItem(itemStack, APPLE_ITEM)) return;

    new RepeatCheckTask(
            RepeatCheckTask.CheckMode.CONTINUOUS, itemDrop::isValid, () -> transformItem(itemDrop))
        .start(CONVERT_SECONDS.get());
  }

  private void transformItem(Item itemDrop) {
    // Create a potion that gives nausea for 10 seconds
    ItemStack potionItem = new ItemStack(Material.POTION);
    Potion potion = new Potion(PotionType.WATER);
    potion.setSplash(false);
    potion.setType(PotionType.WEAKNESS);
    potion.apply(potionItem);

    potionItem.setItemMeta(potionItem.getItemMeta());

    POTION_META.set(potionItem, true);
    itemDrop.setItemStack(potionItem);
  }

  @EventHandler
  public void onPlayerDrinkPotion(PlayerItemConsumeEvent event) {
    ItemStack consumed = event.getItem();
    Player player = event.getPlayer();

    if (consumed.getType() != Material.POTION) return;

    boolean isPotion = POTION_META.has(consumed);
    if (!isPotion) return;

    player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 0));
    reward(event.getPlayer());
  }
}
