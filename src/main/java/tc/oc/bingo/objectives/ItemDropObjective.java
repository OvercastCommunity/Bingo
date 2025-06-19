package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tc.oc.bingo.util.RepeatCheckTask;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.util.MatchPlayers;

@Tracker("item-drop")
public class ItemDropObjective extends ObjectiveTracker {

  private final Map<UUID, UUID> itemDroppers = useState(Scope.MATCH);

  private final Supplier<Material> THROWN_MATERIAL =
      useConfig("thrown-material", Material.WATER_BUCKET);

  private final Supplier<Boolean> TRANSFORM_ON_LAND = useConfig("transform-on-land", true);

  private final Supplier<Material> TRANSFORM_BLOCK = useConfig("transform-block", Material.WATER);

  private final Supplier<Material> TRANSFORM_MATERIAL =
      useConfig("transform-material", Material.BUCKET);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    Material stack = event.getItemDrop().getItemStack().getType();
    if (!stack.equals(THROWN_MATERIAL.get())) return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(player)) return;

    // When logic ends here reward after a short wait
    if (!TRANSFORM_ON_LAND.get()) {
      PGM.get().getExecutor().schedule(() -> reward(event.getPlayer()), 2L, TimeUnit.SECONDS);
      return;
    }

    itemDroppers.put(event.getItemDrop().getUniqueId(), event.getPlayer().getUniqueId());
    getRepeatCheckTask(event).start(5, 20);
  }

  private @NotNull RepeatCheckTask getRepeatCheckTask(PlayerDropItemEvent event) {
    Item droppedItem = event.getItemDrop();

    return new RepeatCheckTask(
        RepeatCheckTask.CheckMode.PASS_ONCE,
        // Wait until item is on the ground
        () -> droppedItem.isValid() && droppedItem.isOnGround(),
        // On success make the block change
        () -> transformItem(droppedItem),
        // Remove from map on failure (set time period elapses)
        () -> itemDroppers.remove(droppedItem.getUniqueId()));
  }

  private void transformItem(Item droppedItem) {
    Block block = droppedItem.getLocation().getBlock();
    UUID playerUUID = itemDroppers.remove(droppedItem.getUniqueId());
    if (playerUUID == null) return;

    MatchPlayer player = getPlayer(playerUUID);
    if (player == null) return;

    // Check if block change possible
    if (!canPlayerPlaceBlock(player, block)) return;

    // Don't replace non-air blocks
    if (!block.isEmpty()) return;

    block.setType(TRANSFORM_BLOCK.get());
    droppedItem.remove();
    droppedItem
        .getWorld()
        .dropItem(droppedItem.getLocation(), new ItemStack(TRANSFORM_MATERIAL.get()));

    reward(player.getBukkit());
  }

  private boolean canPlayerPlaceBlock(MatchPlayer player, Block block) {
    ParticipantState participantState = player.getParticipantState();
    if (participantState == null) return true;

    // Slight bodge to check if player can place the block
    BlockPlaceEvent placeEvent =
        new BlockPlaceEvent(
            block, // The block isn't 'placed' so use the old state
            block.getState(),
            block,
            player.getBukkit().getItemInHand(),
            player.getBukkit(),
            true);

    // Throw bodged event as part of a block transform event
    ParticipantBlockTransformEvent event =
        new ParticipantBlockTransformEvent(
            placeEvent, block, TRANSFORM_BLOCK.get(), participantState);

    // If called event is cancelled placement is invalid
    player.getMatch().callEvent(event);
    return !event.isCancelled();
  }
}
