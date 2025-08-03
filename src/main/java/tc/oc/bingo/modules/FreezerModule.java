package tc.oc.bingo.modules;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.With;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Door;
import tc.oc.bingo.objectives.Scope;
import tc.oc.bingo.util.PGMUtils;

@BingoModule.Config("fridge-recipes")
@BingoModule.AlwaysOn
public class FreezerModule extends BingoModule implements PGMUtils {
  public static final FreezerModule INSTANCE = new FreezerModule();

  private final Supplier<Integer> FREEZE_SECONDS = useConfig("freeze-seconds", 60);
  private final Map<UUID, Freezer> freezers = useState(Scope.PARTICIPATION);

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || notParticipating(event.getPlayer()))
      return;

    var block = getFridge(event.getClickedBlock(), event.getBlockFace());
    if (block == null) return;

    updateDoor(block, true);

    var pl = event.getPlayer();
    var freezer =
        freezers.compute(
            pl.getUniqueId(),
            (k, f) -> {
              if (f == null)
                return new Freezer(block, Bukkit.createInventory(pl, 9, "Freezer"), null);

              if (f.freezingSeconds() > FREEZE_SECONDS.get()) freezeItems(f.inventory());
              return f.withBlock(block);
            });
    pl.openInventory(freezer.inventory());
    pl.playSound(block.getLocation(), Sound.DOOR_OPEN, 1f, 2f);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventoryClose(InventoryCloseEvent event) {
    freezers.computeIfPresent(
        event.getPlayer().getUniqueId(),
        (uuid, f) -> {
          if (!f.inventory().equals(event.getInventory())) return f;

          if (event.getPlayer() instanceof Player p)
            p.playSound(f.block().getLocation(), Sound.DOOR_CLOSE, 1f, 1.5f);
          updateDoor(f.block(), false);

          return f.withClosedAt(Instant.now());
        });
  }

  private void freezeItems(Inventory inventory) {
    for (int i = 0; i < inventory.getSize(); i++) {
      var item = inventory.getItem(i);
      if (item == null) continue;

      // TODO: make custom recipes flexible
      if (item.getType() == Material.WATER_BUCKET) {
        inventory.setItem(i, new ItemStack(Material.ICE));
      }
    }
  }

  private Block getFridge(Block clickedBlock, BlockFace clickedFace) {
    var facing = clickedFace.getOppositeFace();
    var block = getBottomDoor(clickedBlock);
    if (block == null) return null;
    var bottom = getDoorMeta(block);
    var top = getDoorMeta(block.getRelative(BlockFace.UP));

    if (bottom == null || top == null || !top.isTopHalf() || bottom.isTopHalf()) return null;

    if (bottom.isOpen() || facing != bottom.getFacing()) return null;

    var iron = block.getRelative(facing);
    if (iron.getType() != Material.IRON_BLOCK) return null;
    if (iron.getRelative(BlockFace.UP).getType() != Material.IRON_BLOCK) return null;

    return block;
  }

  private Door getDoorMeta(Block block) {
    if (block == null || block.getType() != Material.IRON_DOOR_BLOCK) return null;
    return (Door) block.getState().getMaterialData();
  }

  private Block getBottomDoor(Block block) {
    var state = getDoorMeta(block);
    return state == null ? null : state.isTopHalf() ? block.getRelative(BlockFace.DOWN) : block;
  }

  @SuppressWarnings("deprecation")
  private void updateDoor(Block block, boolean open) {
    var door = getDoorMeta(block);
    if (door == null || door.isOpen() == open) return;
    door.setOpen(open);
    block.setData(door.getData());
  }

  @With
  record Freezer(Block block, Inventory inventory, Instant closedAt) {
    long freezingSeconds() {
      if (closedAt == null) return -1;
      return Duration.between(closedAt, Instant.now()).toSeconds();
    }
  }
}
