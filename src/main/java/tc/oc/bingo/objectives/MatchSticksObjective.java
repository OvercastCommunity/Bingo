package tc.oc.bingo.objectives;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import tc.oc.bingo.modules.ItemRemoveCanceller;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;

@Tracker("match-sticks")
public class MatchSticksObjective extends ObjectiveTracker {

  @Override
  public void enable() {
    super.enable();
    Bukkit.getServer().addRecipe(getRecipe());
  }

  public FurnaceRecipe getRecipe() {
    ItemStack result = new ItemStack(Material.BLAZE_ROD);
    ItemRemoveCanceller.applyCustomMeta(result);
    return new FurnaceRecipe(result, Material.STICK);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    event.getWorld().addRecipe(getRecipe());
  }

  @Override
  public void disable() {
    super.disable();
    Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
    // TODO: recipeIterator.
  }
}
