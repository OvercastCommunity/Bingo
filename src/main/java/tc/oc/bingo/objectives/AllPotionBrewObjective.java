package tc.oc.bingo.objectives;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import tc.oc.bingo.modules.CustomPotionsModule;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;

@Tracker("all-potion-brew")
@DependsOn(CustomPotionsModule.class)
public class AllPotionBrewObjective extends ObjectiveTracker.StatefulSet<String> {

  private static final List<ShapedRecipe> RECIPES =
      IntStream.range(0, 16)
          .mapToObj(
              value ->
                  new ShapedRecipe(new ItemStack(Material.GLASS_BOTTLE, 3))
                      .shape("G G", " G ")
                      .setIngredient('G', Material.STAINED_GLASS, value))
          .toList();

  private final Supplier<Integer> REQUIRED_AMOUNT = useConfig("required-amount", 5);

  @Override
  public void enable() {
    super.enable();
    RECIPES.forEach(Bukkit::addRecipe);
  }

  @EventHandler
  public void onCustomPotionBrew(CustomPotionsModule.CustomPotionBrewEvent event) {
    String customPotionType = CustomPotionsModule.isCustomPotion(event.getItemStack());
    if (customPotionType == null) return;

    trackProgress(event.getPlayer(), customPotionType);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    RECIPES.forEach(event.getWorld()::addRecipe);
  }

  @Override
  protected int maxCount() {
    return REQUIRED_AMOUNT.get();
  }
}
