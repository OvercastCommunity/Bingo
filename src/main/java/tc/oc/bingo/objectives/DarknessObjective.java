package tc.oc.bingo.objectives;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

@Tracker("darkness-bringer")
public class DarknessObjective extends ObjectiveTracker.Stateful<Set<Integer>> {

  private final Supplier<Integer> REQUIRED_LIGHT_SOURCES = useConfig("required-light-sources", 5);

  private static final Set<Material> LIGHT_SOURCES =
      EnumSet.of(
          Material.TORCH,
          Material.REDSTONE_TORCH_ON,
          Material.REDSTONE_LAMP_ON,
          Material.GLOWSTONE,
          Material.SEA_LANTERN,
          Material.BURNING_FURNACE,
          Material.JACK_O_LANTERN,
          Material.BEACON,
          Material.GLOWING_REDSTONE_ORE
          // Not including:
          // - Mushroom
          // - Brewing stand
          // - Dragon egg
          // - Ender chest
          // - Powered comparator
          );

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    Block block = event.getBlock();
    Material blockType = block.getType();

    if (!LIGHT_SOURCES.contains(blockType)) return;
    if (storeMaterial(player, blockType)) reward(player);
  }

  public boolean storeMaterial(Player player, Material material) {
    if (player == null) return false;

    return updateObjectiveData(
                player.getUniqueId(),
                idx -> {
                  idx.add(material.getId());
                  return idx;
                })
            .size()
        >= REQUIRED_LIGHT_SOURCES.get();
  }

  @Override
  public @NotNull Set<Integer> initial() {
    return new HashSet<>();
  }

  @Override
  public @NotNull Set<Integer> deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Arrays.stream(string.split(",")).map(Integer::valueOf).collect(Collectors.toSet());
  }

  @Override
  public @NotNull String serialize(@NotNull Set<Integer> data) {
    return String.join(",", Iterables.transform(data, Object::toString));
  }

  @Override
  public double progress(Set<Integer> data) {
    return (double) data.size() / REQUIRED_LIGHT_SOURCES.get();
  }
}
