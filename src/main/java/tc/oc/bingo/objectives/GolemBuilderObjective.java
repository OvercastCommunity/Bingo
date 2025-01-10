package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

@Tracker("golem-builder")
public class GolemBuilderObjective extends ObjectiveTracker {

  private final Supplier<Set<Material>> HEAD =
      useConfig(
          "head-material", Set.of(Material.JACK_O_LANTERN, Material.PUMPKIN), MATERIAL_SET_READER);
  private final Supplier<Material> BODY = useConfig("body-material", Material.HAY_BLOCK);
  private final Supplier<Material> ARMS = useConfig("arm-material", Material.LEVER);
  private final Supplier<Material> LEGS = useConfig("legs-material", Material.FENCE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Block placedBlock = event.getBlock();
    if (!HEAD.get().contains(placedBlock.getType())) return;

    Block bodyBlock = placedBlock.getRelative(BlockFace.DOWN);
    if (bodyBlock.getType() != BODY.get()) return;

    boolean hasValidArms =
        (bodyBlock.getRelative(BlockFace.EAST).getType() == ARMS.get()
                && bodyBlock.getRelative(BlockFace.WEST).getType() == ARMS.get())
            || (bodyBlock.getRelative(BlockFace.NORTH).getType() == ARMS.get()
                && bodyBlock.getRelative(BlockFace.SOUTH).getType() == ARMS.get());

    if (!hasValidArms && ARMS.get() != Material.AIR) return;

    Block legsBlock = bodyBlock.getRelative(BlockFace.DOWN);
    if (legsBlock.getType() != LEGS.get() && LEGS.get() != Material.BARRIER) return;

    reward(event.getPlayer());
  }
}
