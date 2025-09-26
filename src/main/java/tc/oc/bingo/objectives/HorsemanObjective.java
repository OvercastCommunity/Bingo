package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityMountEvent;

@Tracker("horseman")
public class HorsemanObjective extends ObjectiveTracker {

  // Riding a horse with a pumpkin on your head, headless horseman.

  private final Supplier<Material> HEAD_MATERIAL = useConfig("head-material", Material.PUMPKIN);
  private final Supplier<EntityType> ENTITY_TYPE = useConfig("entity-type", EntityType.HORSE);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRideEntity(EntityMountEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;

    ItemStack helmet = player.getEquipment().getHelmet();
    if (helmet == null) return;

    if (!helmet.getType().equals(HEAD_MATERIAL.get())) return;

    if (event.getMount().getType().equals(ENTITY_TYPE.get())) {
      reward(player);
    }
  }
}
