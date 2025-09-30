package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Pumpkin;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("block-glow-up")
public class BlockGlowUpObjective extends ObjectiveTracker {

  private final Supplier<Material> REQUIRED_BLOCK = useConfig("required-block", Material.PUMPKIN);
  private final Supplier<Material> CONVERTED_BLOCK =
      useConfig("converted-block", Material.JACK_O_LANTERN);

  private final Supplier<Material> HELD_ITEM = useConfig("required-item", Material.TORCH);

  private final Supplier<Boolean> REQUIRE_DIRECTIONAL = useConfig("require-directional", true);

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(player)) return;

    // Must be the required block and holding the required item
    Block block = event.getClickedBlock();
    if (block.getType() != REQUIRED_BLOCK.get()) return;
    if (event.getItem() == null || event.getItem().getType() != HELD_ITEM.get()) return;

    // Directional check if enabled
    if (REQUIRE_DIRECTIONAL.get() && block.getState().getMaterialData() instanceof Directional d) {
      // Player must click on the "front" face
      if (event.getBlockFace() != getDirectionalFacing(d).getOppositeFace()) return;
    }

    // Replace with converted block, preserving facing if possible
    BlockState state = block.getState();
    MaterialData oldData = state.getMaterialData();

    block.setType(CONVERTED_BLOCK.get());
    BlockState newState = block.getState();

    if (oldData instanceof Directional oldBlock
        && newState.getMaterialData() instanceof Directional newBlock) {
      newBlock.setFacingDirection(getDirectionalFacing(oldBlock));
      newState.update(true, false);
    }

    event.setCancelled(true);

    // Consume the held item
    InventoryUtils.consumeItem(event);

    // Play an effect sound/particle
    block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.DIG_WOOD, 1.0f, 1.0f);

    // Spawn flame effect at block center
    block
        .getWorld()
        .playEffect(block.getLocation().add(0.5, 0.5, 0.5), Effect.MOBSPAWNER_FLAMES, 0);

    // Reward the player
    reward(event.getPlayer());
  }

  public BlockFace getDirectionalFacing(Directional directional) {
    if (!(directional instanceof Pumpkin p)) {
      return directional.getFacing();
    }

    // Pumpkins have an incorrect facing mapping in Bukkit
    return switch (p.getData()) {
      case 0 -> BlockFace.NORTH;
      case 1 -> BlockFace.EAST;
      case 2 -> BlockFace.SOUTH;
      default -> BlockFace.WEST;
    };
  }
}
