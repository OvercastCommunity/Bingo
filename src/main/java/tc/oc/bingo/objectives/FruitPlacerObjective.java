package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerBlockTransformEvent;
import tc.oc.pgm.util.material.Materials;

@Tracker("fruit-placer")
public class FruitPlacerObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> TRACKED_ITEM = CustomItem.of("peach");

  private final Supplier<Integer> REQUIRED_PLACE = useConfig("required-placements", 5);

  private final Map<UUID, Integer> placedItems = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlaceEvent(BlockPlaceEvent event) {
    if (!CustomItemModule.isCustomItem(event.getItemInHand(), TRACKED_ITEM)) return;
    Player player = event.getPlayer();

    if (Materials.isWater(event.getBlockReplacedState())) return;

    Integer placedCount =
        placedItems.compute(player.getUniqueId(), (uuid, count) -> (count == null) ? 1 : count + 1);

    if (placedCount >= REQUIRED_PLACE.get()) {
      reward(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockBreak(PlayerBlockTransformEvent event) {
    if (!event.isBreak()) return;

    Block block = event.getBlock();
    MatchPlayer matchPlayer = event.getPlayer();
    if (matchPlayer == null) return;

    if (CustomItemModule.isCustomBlock(block, TRACKED_ITEM)) {
      UUID playerUUID = matchPlayer.getId();
      placedItems.computeIfPresent(playerUUID, (uuid, count) -> (count > 0) ? count - 1 : 0);
    }
  }

  @Override
  public Double getProgress(UUID uuid) {
    // Requires setting Bingo progress manually (like above) to be used
    return (double) placedItems.getOrDefault(uuid, 0) / REQUIRED_PLACE.get();
  }
}
