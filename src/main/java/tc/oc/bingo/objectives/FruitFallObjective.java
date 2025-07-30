package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.util.CustomItem;

@Tracker("fruit-fall")
public class FruitFallObjective extends ObjectiveTracker.StatefulSet<String> {

  private static final Supplier<CustomItem> ORANGE_ITEM = CustomItem.of("orange");
  private static final Supplier<CustomItem> APPLE_ITEM = CustomItem.of("apple");
  private static final Supplier<CustomItem> PEAR_ITEM = CustomItem.of("pear");
  private static final Supplier<CustomItem> LEMON_ITEM = CustomItem.of("lemon");
  private static final Supplier<CustomItem> PEACH_ITEM = CustomItem.of("peach");
  private static final Supplier<CustomItem> CHERRY_ITEM = CustomItem.of("cherry");
  private static final Supplier<CustomItem> STRAWBERRY_ITEM = CustomItem.of("strawberry");

  private static final Set<Supplier<CustomItem>> TREE_FRUITS =
      Set.of(ORANGE_ITEM, APPLE_ITEM, PEAR_ITEM, LEMON_ITEM, PEACH_ITEM);
  private static final Set<Supplier<CustomItem>> GROUND_FRUITS =
      Set.of(CHERRY_ITEM, STRAWBERRY_ITEM);

  private final Set<Material> SWORD_MATERIALS =
      EnumSet.of(
          Material.WOOD_SWORD,
          Material.STONE_SWORD,
          Material.GOLD_SWORD,
          Material.IRON_SWORD,
          Material.DIAMOND_SWORD);

  private static final Set<Material> LEAVES = Set.of(Material.LEAVES, Material.LEAVES_2);

  private final Supplier<Boolean> REQUIRE_SWORD = useConfig("require-sword", true);
  private final Supplier<Integer> DROPS_REQUIRED = useConfig("drops-required", 5);
  private final Supplier<Double> DROP_CHANCE = useConfig("drop-chance", 1d); // 0.1d

  // When a player breaks a leaf block, they have a chance to drop a fruit.
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();

    // Check that the block is a leaf
    Material type = block.getType();
    if (!LEAVES.contains(type)) return;

    if (REQUIRE_SWORD.get()
        && !SWORD_MATERIALS.contains(event.getPlayer().getItemInHand().getType())) return;

    // Require no drops from block (i.e sheared)
    if (!block.getDrops().isEmpty()) return;

    Block relative = block.getRelative(BlockFace.DOWN);
    boolean isTree = relative.isEmpty() || relative.getType().isTransparent();

    // Random chance to drop an item
    if (Math.random() > DROP_CHANCE.get()) return;

    Supplier<CustomItem> fruit =
        (isTree)
            ? TREE_FRUITS.stream()
                .skip((int) (Math.random() * TREE_FRUITS.size()))
                .findFirst()
                .orElse(null)
            : GROUND_FRUITS.stream()
                .skip((int) (Math.random() * GROUND_FRUITS.size()))
                .findFirst()
                .orElse(null);

    // Create the item stack and drop it
    if (fruit == null) return;
    CustomItem customItem = fruit.get();
    ItemStack itemStack = customItem.toItemStack();
    block.getWorld().dropItem(block.getLocation(), itemStack);
    trackProgress(event.getPlayer(), customItem.id());
  }

  @Override
  protected int maxCount() {
    return DROPS_REQUIRED.get();
  }
}
