package tc.oc.bingo.modules;

import static tc.oc.bingo.modules.ItemRemoveCanceller.ITEM_META;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.TileEntityBrewingStand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.objectives.Scope;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.util.event.ItemTransferEvent;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.reflect.ReflectionUtils;

@BingoModule.Config("custom-items")
public class CustomPotionsModule extends BingoModule {

  public static final CustomPotionsModule INSTANCE = new CustomPotionsModule();

  public static final Field EFFECTS_FIELD =
      ReflectionUtils.getField(
          "org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaPotion", "customEffects");

  private final Map<String, CustomPotionType> registeredPotions = new HashMap<>();

  private final Map<UUID, Map<String, Instant>> activeEffects = useState(Scope.LIFE);

  private final Map<Vector, PotionBrewTask> activeStands = new HashMap<>();

  private static final ItemTag<String> CUSTOM_POTION = ItemTag.newString("custom-potion");

  @Override
  public Stream<ManagedListener> children() {
    return Stream.concat(
        super.children(),
        Stream.of(new ManagedListener.Ticker(this::tickPotions, 0, 500, TimeUnit.MILLISECONDS)));
  }

  public boolean registerPotion(String name, CustomPotionType potionType) {
    if (registeredPotions.containsKey(name)) return false;
    registeredPotions.put(name, potionType);
    return true;
  }

  public boolean removePotion(String name) {
    return registeredPotions.remove(name) != null;
  }

  private void tickPotions() {
    Instant now = Instant.now();
    for (Map<String, Instant> effects : activeEffects.values()) {
      effects.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
  }

  public static boolean hasEffect(Player player, String potionSlug) {
    if (INSTANCE == null) return false;
    Map<String, Instant> effects = INSTANCE.activeEffects.get(player.getUniqueId());
    if (effects == null) return false;
    return (effects.get(potionSlug) != null);
  }

  public static @Nullable String isCustomPotion(ItemStack item) {
    if (item == null) return null;
    return CUSTOM_POTION.get(item);
  }

  public static boolean isCustomPotion(ItemStack item, String slug) {
    if (item == null) return false;
    String potionSlug = CUSTOM_POTION.get(item);

    return (potionSlug != null && potionSlug.equals(slug));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onMatchFinish(MatchFinishEvent event) {
    activeEffects.clear();

    List.copyOf(activeStands.values()).forEach(PotionBrewTask::cancel);
    activeStands.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    if (event.getItem().getType() == Material.MILK_BUCKET) {
      // Clear all active effects
      Map<String, Instant> remove = INSTANCE.activeEffects.remove(event.getPlayer().getUniqueId());
      if (remove == null || remove.isEmpty()) return;

      event.getPlayer().sendMessage("§6You feel purified.");
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBrew(BrewEvent event) {
    BrewerInventory contents = event.getContents();
    ItemStack ingredient = contents.getIngredient();
    if (ingredient == null) return;

    // Cancel the brew if the ingredient is a custom item
    if (ITEM_META.has(ingredient)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPotionConsume(PlayerItemConsumeEvent event) {
    if (event.getItem().getType().equals(Material.POTION)) {

      @Nullable String potionType = CUSTOM_POTION.get(event.getItem());
      if (potionType == null) return;

      event
          .getPlayer()
          .sendMessage(
              "§7You feel the effects of the "
                  + event.getItem().getItemMeta().getDisplayName()
                  + "§7!");

      // Apply effect for 30 seconds
      activeEffects
          .computeIfAbsent(event.getPlayer().getUniqueId(), k -> new java.util.HashMap<>())
          .put(potionType, Instant.now().plusSeconds(30));

      Bukkit.getPluginManager()
          .callEvent(new CustomPotionDrinkEvent(event.getPlayer(), event.getItem(), potionType));
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onItemTransfer(ItemTransferEvent event) {
    if (!(event.getTo() instanceof BrewerInventory inventory)) return;
    Player player = (event.getActor() instanceof Player p) ? p : null;

    PGM.get()
        .getExecutor()
        .schedule(() -> tryStartBrewing(inventory, player), 50, TimeUnit.MILLISECONDS);
  }

  public void tryStartBrewing(BrewerInventory inventory, @Nullable Player actor) {
    ItemStack ingredient = inventory.getIngredient();
    if (ingredient == null) return;

    CustomPotionType customPotion =
        registeredPotions.values().stream()
            .filter(potionType -> potionType.checkIngredient(ingredient))
            .findFirst()
            .orElse(null);

    if (customPotion == null) return;

    // TODO: tidy up checks and reuse here and in run tick

    // Verify that the bottles are all valid awkward potions
    if (!hasAnyAwkwardPotions(inventory)) return;

    // Check if there's a potion of the same type already brewing
    PotionBrewTask currentTask = activeStands.get(inventory.getHolder().getLocation().toVector());
    if (currentTask != null && currentTask.potion == customPotion) {
      return;
    }

    // At this point, we can start brewing
    new PotionBrewTask(customPotion, inventory, actor);
  }

  private static final Set<ClickType> CLICK_TYPES =
      Set.of(ClickType.LEFT, ClickType.SHIFT_LEFT, ClickType.RIGHT, ClickType.SHIFT_RIGHT);

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBrewInsert(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory() instanceof BrewerInventory inventory)) return;

    PotionBrewTask currentTask = activeStands.get(inventory.getHolder().getLocation().toVector());
    if (currentTask != null
        && currentTask.player != null
        && !event.getActor().equals(currentTask.player)) {
      event.setCancelled(true);
      event.getActor().sendMessage("§cThe brewing stand is already in use! Let them cook.");
      return;
    }

    if (!CLICK_TYPES.contains(event.getClick())) return;

    if (event.isShiftClick()) {
      if (!event.getClickedInventory().equals(event.getView().getBottomInventory())) return;
      ItemStack clicked = event.getCurrentItem();
      if (clicked == null) return;

      CustomPotionType customPotion =
          registeredPotions.values().stream()
              .filter(potionType -> potionType.checkIngredient(clicked))
              .findFirst()
              .orElse(null);

      if (customPotion == null) return;

      ItemStack ingredient = inventory.getIngredient();
      if (ingredient != null
          && !ingredient.getType().equals(Material.AIR)
          && ingredient.getAmount() > 0) return;

      // event.getClickedInventory().setItem(event.getSlot(), null);
      inventory.setIngredient(clicked);
      event.getView().setItem(event.getRawSlot(), null);
      event.setCancelled(true);
    } else {
      // Only check the ingredient slot (slot 3)
      if (event.getSlot() != 3) return;

      ItemStack clicked = event.getCursor();
      if (clicked == null) return;

      CustomPotionType customPotion =
          registeredPotions.values().stream()
              .filter(potionType -> potionType.checkIngredient(clicked))
              .findFirst()
              .orElse(null);

      if (customPotion == null) return;

      ItemStack cursor = event.getCursor();
      ItemStack currentItem = event.getCurrentItem();

      event.setCursor(currentItem);
      event.setCurrentItem(cursor);
    }

    event.setCancelled(true);

    tryStartBrewing(inventory, event.getActor());

    // At this point, we can start brewing

    // TODO: event.getActor();
  }

  private boolean hasAnyAwkwardPotions(BrewerInventory inv) {
    for (int i = 0; i < 3; i++) {
      ItemStack bottle = inv.getItem(i);
      if (bottle != null && bottle.getType() == Material.POTION && bottle.getDurability() == 16) {
        return true;
      }
    }

    return false;
  }

  public static class CustomPotionBrewEvent extends Event {

    private final Player player;
    private final ItemStack itemStack;

    private static final HandlerList handlers = new HandlerList();

    public CustomPotionBrewEvent(Player player, ItemStack itemStack) {
      this.player = player;
      this.itemStack = itemStack;
    }

    public Player getPlayer() {
      return player;
    }

    public ItemStack getItemStack() {
      return itemStack;
    }
  }

  public static class CustomPotionDrinkEvent extends Event {

    private final Player player;
    private final ItemStack itemStack;
    private final String slug;

    private static final HandlerList handlers = new HandlerList();

    public CustomPotionDrinkEvent(Player player, ItemStack itemStack, String slug) {
      this.player = player;
      this.itemStack = itemStack;
      this.slug = slug;
    }

    public Player getPlayer() {
      return player;
    }

    public ItemStack getItemStack() {
      return itemStack;
    }

    public String getSlug() {
      return slug;
    }
  }

  public static ItemStack createPotion(String name, List<String> lore, short durability) {
    ItemStack potion = new ItemStack(Material.POTION, 1, durability);
    PotionMeta meta = (PotionMeta) potion.getItemMeta();
    meta.setDisplayName(name);
    meta.setLore(lore);
    meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
    CustomPotionsModule.setEmptyEffects(meta);

    potion.setItemMeta(meta);
    return potion;
  }

  public static void setEmptyEffects(PotionMeta meta) {
    ReflectionUtils.setField(meta, List.of(), CustomPotionsModule.EFFECTS_FIELD);
  }

  public record CustomPotionType(
      String slug, Function<ItemStack, Boolean> ingredientMatcher, Supplier<ItemStack> effect) {

    public boolean checkIngredient(ItemStack itemStack) {
      if (itemStack == null) return false;

      return ingredientMatcher.apply(itemStack);
    }
  }

  class PotionBrewTask extends BukkitRunnable {

    private final CustomPotionType potion;
    private final Block block;
    private final BrewerInventory inv;
    private final TileEntityBrewingStand stand;
    private final Player player;
    int time = 400;

    public PotionBrewTask(CustomPotionType potionType, BrewerInventory inventory, Player player) {
      this.potion = potionType;
      this.block = inventory.getHolder().getBlock();
      this.inv = inventory;
      this.player = player;

      BlockPosition pos = new BlockPosition(block.getX(), block.getY(), block.getZ());
      this.stand =
          (TileEntityBrewingStand)
              ((CraftWorld) inv.getHolder().getWorld()).getHandle().getTileEntity(pos);

      Vector vector = block.getLocation().toVector();
      PotionBrewTask remove = activeStands.remove(vector);
      if (remove != null) {
        remove.cancel();
      }
      activeStands.put(vector, this);

      this.runTaskTimer(Bingo.get(), 0, 1);
    }

    @Override
    public void cancel() {
      Vector vector = this.block.getLocation().toVector();
      activeStands.remove(vector, this);

      super.cancel();
    }

    @Override
    public void run() {
      time--;
      if (time < 0) {
        // Allow a few ticks of leeway to collect items
        if (time < -60) {
          this.cancel();
        }
        return;
      }

      if (!block.getType().equals(Material.BREWING_STAND)) {
        this.cancel();
        return;
      }

      // If the inventory item changes typed, cancel the brew
      if (!potion.checkIngredient(inv.getIngredient())) {
        this.cancel();
        return;
      }

      if (!hasAnyAwkwardPotions(inv)) {
        this.cancel();
        return;
      }

      if (time == 0) {
        // Brewing finished so replace bottles
        ItemStack itemStack = potion.effect.get();
        CUSTOM_POTION.set(itemStack, potion.slug);

        for (int i = 0; i < 3; i++) {
          ItemStack bottle = inv.getItem(i);
          if (bottle != null && bottle.getType() == Material.POTION) {
            inv.setItem(i, itemStack);
          }
        }
        // Clear ingredient
        ItemStack ingredient = inv.getIngredient();
        ingredient.setAmount(ingredient.getAmount() - 1);
        inv.setIngredient(ingredient);

        Bukkit.getPluginManager().callEvent(new CustomPotionBrewEvent(player, itemStack));
      }

      // Tick down animation
      stand.brewTime = time;
      stand.update();
    }
  }
}
