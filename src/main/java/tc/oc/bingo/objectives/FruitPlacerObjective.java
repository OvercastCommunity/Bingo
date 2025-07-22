package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;

@Tracker("fruit-placer")
public class FruitPlacerObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> TRACKED_ITEM = CustomItem.of("peach");

  private final Supplier<Integer> REQUIRED_PLACE = useConfig("required-placements", 5);

  private final Map<UUID, Integer> placedItems = useState(Scope.MATCH);

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlaceEvent(BlockPlaceEvent event) {
    if (!CustomItemModule.isCustomItem(event.getItemInHand(), TRACKED_ITEM)) return;
    Player player = event.getPlayer();

    Integer placedCount =
        placedItems.compute(player.getUniqueId(), (uuid, count) -> (count == null) ? 1 : count + 1);

    if (placedCount >= REQUIRED_PLACE.get()) {
      reward(player);
    }
  }

  // TODO: track removals too (so cant break and place) subtract?

  @Override
  public Double getProgress(UUID uuid) {
    return (double) placedItems.getOrDefault(uuid, 0) / REQUIRED_PLACE.get();
  }
}
