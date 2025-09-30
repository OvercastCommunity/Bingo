package tc.oc.bingo.objectives;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomPotionsModule;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.bingo.modules.FreezerModule;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

@Tracker("frost-walker-potion-task")
@DependsOn(FreezerModule.class)
public class FrostWalkerPotionObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_BLOCKS = useConfig("required-blocks", 100);
  private final Supplier<Integer> ICE_DURATION = useConfig("ice-duration", 4000);
  private final Supplier<Integer> ICE_RADIUS = useConfig("radius", 2);

  private final Map<UUID, Integer> playerIceBlocks = useState(Scope.LIFE);
  private final Map<Location, Long> placedBlocks = Maps.newConcurrentMap();

  @Override
  public Stream<ManagedListener> children() {
    return Stream.concat(
        super.children(),
        Stream.of(
            new ManagedListener.Ticker(this::convertIceBlocks, 0, 500, TimeUnit.MILLISECONDS)));
  }

  @Override
  public void setupDependencies() {
    CustomPotionsModule.CustomPotionType customPotionType =
        new CustomPotionsModule.CustomPotionType(
            "frost", this::checkIngredient, this::createPotion);

    CustomPotionsModule.INSTANCE.registerPotion("frost", customPotionType);
  }

  @Override
  public void teardownDependencies() {
    CustomPotionsModule.INSTANCE.removePotion("frost");
  }

  private Boolean checkIngredient(ItemStack itemStack) {
    return itemStack.getType().equals(Material.ICE);
  }

  private ItemStack createPotion() {
    return CustomPotionsModule.createPotion(
        "§bPotion of Frost", List.of("§9Frost (00:30)"), (short) 2);
  }

  private void convertIceBlocks() {
    if (!placedBlocks.isEmpty()) {
      long now = System.currentTimeMillis();
      Iterator<Map.Entry<Location, Long>> iterator = placedBlocks.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Location, Long> entry = iterator.next();
        if (now - entry.getValue() > ICE_DURATION.get()) {
          entry.getKey().getBlock().setType(Material.STATIONARY_WATER);
          iterator.remove();
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    if (!CustomPotionsModule.hasEffect(event.getPlayer(), "frost")) {
      return;
    }

    if (LocationUtils.stoodInMaterial(event.getTo(), Material.STATIONARY_WATER)) return;

    Player player = event.getPlayer();
    if (!player.isOnGround()) return;

    Block block = event.getTo().getBlock().getRelative(BlockFace.DOWN);
    Location center = block.getLocation();

    List<Location> changedBlocks = new ArrayList<>();

    for (int x = -2; x <= 2; x++) { // straight radius 3
      for (int z = -2; z <= 2; z++) {
        // Skip far diagonals (distance >1 diagonally)
        if (Math.abs(x) > 1 && Math.abs(z) > 1) continue;

        Location checkLoc = center.clone().add(x, 0, z);
        Block checkBlock = checkLoc.getBlock();

        if (checkBlock.getType() == Material.STATIONARY_WATER) {
          checkBlock.setType(Material.ICE, false);
          placedBlocks.put(checkLoc, System.currentTimeMillis());
          changedBlocks.add(checkLoc);
        }
      }
    }

    // After looping, update player ice count and reward
    int newBlocks = changedBlocks.size();
    if (newBlocks > 0) {
      int count =
          playerIceBlocks.compute(
              player.getUniqueId(), (k, v) -> v == null ? newBlocks : v + newBlocks);

      if (count >= REQUIRED_BLOCKS.get()) {
        reward(player);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (placedBlocks.containsKey(event.getBlock().getLocation())) {
      event.setCancelled(true);
      event.getBlock().setType(Material.STATIONARY_WATER);
      placedBlocks.remove(event.getBlock().getLocation());
    }
  }

  @Override
  public Double getProgress(UUID uuid) {
    return computeProgress(playerIceBlocks.get(uuid), REQUIRED_BLOCKS.get());
  }
}
