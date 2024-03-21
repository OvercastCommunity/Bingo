package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;

@Tracker("player-craft")
public class PlayerCraftObjective extends ObjectiveTracker {

  public Material materialRequired = Material.PISTON_BASE;

  @Override
  public void setConfig(ConfigurationSection config) {
    String name = config.getString("material-name", "PISTON_BASE");
    Material material = Material.getMaterial(name);
    if (material != null) materialRequired = material;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraftEvent(CraftItemEvent event) {
    if (event.getRecipe().getResult().getType().equals(materialRequired)) {
      reward(event.getActor().getPlayer());
    }
  }
}
