package tc.oc.bingo.objectives;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.MaterialData;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.util.material.Materials;

@Tracker("fruit-crafting")
public class FruitCraftingObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> STRAWBERRY_ITEM = CustomItem.of("strawberry");
  private static final Supplier<CustomItem> STRAWBERRY_RESULT_ITEM = CustomItem.of("strawberries_and_cream");

  ShapelessRecipe shapelessRecipe =
      new ShapelessRecipe(STRAWBERRY_RESULT_ITEM.get().toItemStack())
          .addIngredient(Material.MILK_BUCKET)
          .addIngredient(new MaterialData(Materials.PLAYER_HEAD, (byte) 3))
          .addIngredient(new MaterialData(Materials.PLAYER_HEAD, (byte) 3));

  @Override
  public void enable() {
    super.enable();
    Bukkit.getServer().addRecipe(shapelessRecipe);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    event.getWorld().addRecipe(shapelessRecipe);
  }

  @Override
  public void disable() {
    super.disable();
    Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
    // TODO: recipeIterator.
  }

  private static Boolean fruitCheck(ItemStack item) {
    return CustomItemModule.isCustomItem(item, STRAWBERRY_ITEM);
  }

  @EventHandler(ignoreCancelled = false)
  public void onItemCraft(PrepareItemCraftEvent event) {
    ItemStack[] contents = event.getInventory().getContents();

    Map<Function<ItemStack, Boolean>, Boolean> conditions =
        Map.of(
            FruitCraftingObjective::fruitCheck,
            false,
            FruitCraftingObjective::fruitCheck,
            false,
            item -> item != null && item.getType().equals(Material.MILK_BUCKET),
            false);

    for (ItemStack content : contents) {
      if (content == null || content.getType() == Material.AIR) continue;
      for (Map.Entry<Function<ItemStack, Boolean>, Boolean> entry : conditions.entrySet()) {
        // If already met, skip to next condition
        if (entry.getValue()) continue;
        Function<ItemStack, Boolean> condition = entry.getKey();
        if (condition.apply(content)) {
          entry.setValue(true);
          break;
        }
      }
    }

    boolean allConditionsMet = conditions.values().stream().allMatch(Boolean::booleanValue);
    if (!allConditionsMet) return;

    reward(event.getActor());
  }
}
