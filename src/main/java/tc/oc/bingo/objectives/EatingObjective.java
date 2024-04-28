package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@Tracker("eating")
public class EatingObjective extends ObjectiveTracker {

  private final Supplier<Integer> FOODS_REQUIRED = useConfig("foods-required", 3);

  private final Map<UUID, Set<Material>> consumedIds = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Material item = event.getItem().getType();
    if (item.isEdible()) {
      UUID playerUUID = event.getPlayer().getUniqueId();

      Set<Material> consumedItems =
          consumedIds.computeIfAbsent(playerUUID, id -> EnumSet.noneOf(Material.class));

      if (consumedItems.add(item) && consumedItems.size() >= FOODS_REQUIRED.get())
        reward(event.getPlayer());
    }
  }
}
