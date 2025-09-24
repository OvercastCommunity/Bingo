package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomPotionsModule;

@Tracker("greed-potion-task")
public class GreedPotionObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_AMOUNT = useConfig("required-amount", 10);

  private final Set<Material> EXTRA_LOOT_BLOCKS =
      EnumSet.of(Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.GOLD_ORE, Material.IRON_ORE);

  @Override
  public void setupDependencies() {
    CustomPotionsModule.CustomPotionType customPotionType =
        new CustomPotionsModule.CustomPotionType(
            "greed", this::checkIngredient, this::createPotion);

    CustomPotionsModule.INSTANCE.registerPotion("greed", customPotionType);
  }

  @Override
  public void teardownDependencies() {
    CustomPotionsModule.INSTANCE.removePotion("greed");
  }

  private Boolean checkIngredient(ItemStack itemStack) {
    return (itemStack.getType().equals(Material.GOLD_NUGGET));
  }

  private ItemStack createPotion() {
    return CustomPotionsModule.createPotion(
        "§6Potion of Greed", List.of("§9Greed (00:30)", "§7Makes you rich… maybe."), (short) 3);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBlockBreak(BlockBreakEvent event) {
    // Check player is playing
    Block block = event.getBlock();
    if (!EXTRA_LOOT_BLOCKS.contains(block.getType())) return;

    if (!CustomPotionsModule.hasEffect(event.getPlayer(), "greed")) return;

    // Make the block drop extra loot
    Location dropLocation = block.getLocation().add(0, 0.5, 0);
    block.getWorld().dropItemNaturally(dropLocation, new ItemStack(event.getBlock().getType(), 1));

    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_AMOUNT.get();
  }
}
