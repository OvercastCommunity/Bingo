package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
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
  private static final Supplier<CustomItem> STRAWBERRY_RESULT_ITEM =
      CustomItem.of("strawberries_and_cream");

  private ShapelessRecipe shapelessRecipe = null;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    shapelessRecipe =
        new ShapelessRecipe(STRAWBERRY_RESULT_ITEM.get().toItemStack())
            .addIngredient(Material.MILK_BUCKET)
            .addIngredient(new MaterialData(Materials.PLAYER_HEAD, (byte) 3))
            .addIngredient(new MaterialData(Materials.PLAYER_HEAD, (byte) 3));

    event.getWorld().addRecipe(shapelessRecipe);
  }

  @Override
  public void teardownDependencies() {
    if (shapelessRecipe == null) return;
    Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
    while (Bukkit.getServer().recipeIterator().hasNext()) {
      if (recipeIterator.next().equals(shapelessRecipe)) {
        recipeIterator.remove();
        break;
      }
    }
  }

  private static Boolean fruitCheck(ItemStack item) {
    return CustomItemModule.isCustomItem(item, STRAWBERRY_ITEM);
  }

  @EventHandler(ignoreCancelled = true)
  public void onItemCraft(PrepareItemCraftEvent event) {
    if (shapelessRecipe == null) return;

    CraftingInventory inventory = event.getInventory();
    ItemStack[] contents = inventory.getMatrix();

    List<PredicateState> conditions = new ArrayList<>();
    PredicateState fruitCheck = new PredicateState(FruitCraftingObjective::fruitCheck);
    conditions.add(fruitCheck);
    conditions.add(new PredicateState(FruitCraftingObjective::fruitCheck));
    conditions.add(
        new PredicateState(item -> item != null && item.getType() == Material.MILK_BUCKET));

    for (ItemStack content : contents) {
      if (content == null || content.getType() == Material.AIR) continue;
      for (PredicateState entry : conditions) {
        if (entry.passes) continue; // Skip if already passed
        if (entry.condition.apply(content)) {
          entry.passes = true;
          break;
        }
      }
    }

    if (!event.getRecipe().getResult().equals(shapelessRecipe.getResult())) return;

    boolean allConditionsMet = conditions.stream().allMatch(check -> check.passes);

    if (!allConditionsMet) {
      // When the craft has fruit but not the required type
      if (fruitCheck.passes) {
        inventory.setResult(null);
      }
      return;
    }

    reward(event.getActor());
  }

  private static class PredicateState {

    public final Function<ItemStack, Boolean> condition;
    public boolean passes = false;

    public PredicateState(Function<ItemStack, Boolean> condition) {
      this.condition = condition;
    }
  }
}
