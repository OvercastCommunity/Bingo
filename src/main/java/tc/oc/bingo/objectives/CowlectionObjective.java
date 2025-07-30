package tc.oc.bingo.objectives;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("cowlection")
public class CowlectionObjective extends ObjectiveTracker {

  // When a player leashes a cow whilst having another cow leashed reward them

  // TODO: add current leash recipe (out of string) no slime balls

  ShapedRecipe recipe =
      new ShapedRecipe(new ItemStack(Material.LEASH))
          .shape("SSX", "SSX", "XXS")
          .setIngredient('S', Material.STRING)
          .setIngredient('X', Material.AIR);

  @Override
  public void enable() {
    super.enable();
    Bukkit.getServer().addRecipe(recipe);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    event.getWorld().addRecipe(recipe);
  }

  @Override
  public void disable() {
    super.disable();
    Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
    // TODO: recipeIterator.
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerLeashEntityEvent(PlayerLeashEntityEvent event) {
    Player player = event.getPlayer();
    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    if (!(event.getEntity() instanceof Animals actionEntity)) return;

    if (actionEntity.isLeashed()) return;

    Animals otherEntity =
        player.getWorld().getNearbyEntitiesByType(Cow.class, player.getLocation(), 8).stream()
            .filter(entity -> entity.getLeashHolder().equals(player))
            .findFirst()
            .orElse(null);

    if (otherEntity == null) return;

    otherEntity.setLeashHolder(actionEntity);

    event.setCancelled(true);

    // TODO: take the leash from the players inventory
    InventoryUtils.consumeItem(event, player);

    reward(player);
  }
}
