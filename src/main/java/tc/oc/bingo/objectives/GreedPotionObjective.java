package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.bingo.modules.CustomPotionsModule;
import tc.oc.bingo.modules.DependsOn;

@Tracker("greed-potion-task")
@DependsOn(CustomPotionsModule.class)
public class GreedPotionObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_AMOUNT = useConfig("required-amount", 10);
  private final Supplier<Boolean> PUNISH_ON_USE = useConfig("punish-on-use", true);

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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    // Check player is playing
    Block block = event.getBlock();
    if (!EXTRA_LOOT_BLOCKS.contains(block.getType())) return;

    Player player = event.getPlayer();
    if (!CustomPotionsModule.hasEffect(player, "greed")) return;

    // Make the block drop extra loot
    Location dropLocation = block.getLocation().add(0, 0.5, 0);
    block.getWorld().dropItemNaturally(dropLocation, new ItemStack(event.getBlock().getType(), 1));

    if (PUNISH_ON_USE.get()) {
      // Punish player by lowering their max health by 2-4 hearts
      player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 0));

      // Reduce by 2 full hearts until left on 1 heart
      double maxHealth = player.getMaxHealth();
      if (maxHealth >= 1) {
        player.setMaxHealth(Math.max(1, maxHealth - 4));
      }
    }

    trackProgress(player);
  }

  @Override
  protected int maxValue() {
    return REQUIRED_AMOUNT.get();
  }
}
