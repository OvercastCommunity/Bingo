package tc.oc.bingo.objectives;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import tc.oc.bingo.modules.ItemRemoveCanceller;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

@Tracker("match-sticks")
public class MatchSticksObjective extends ObjectiveTracker {

  private final Set<UUID> smelters = Collections.newSetFromMap(useState(Scope.LIFE));

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

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    event.getWorld().addRecipe(getRecipe());
  }

  @Override
  public void disable() {
    super.disable();
    Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
    // TODO: recipeIterator.
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemTransfer(PlayerItemTransferEvent event) {
    // Check that the player put a stick in to the furnace
    if (event.getTo() instanceof FurnaceInventory && event.isRelinquishing()) {
      if (event.getItem().getType() == Material.STICK) {
        smelters.add(event.getPlayer().getUniqueId());
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFurnaceExtract(FurnaceExtractEvent event) {
    if (!smelters.contains(event.getPlayer().getUniqueId())) return;
    if (!event.getItemType().equals(Material.BLAZE_ROD)) return;

    reward(event.getPlayer());
  }
}
