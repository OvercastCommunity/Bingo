package tc.oc.bingo.listeners;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.ConfigHandler;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.wool.WoolMatchModule;

public class ItemRemoveCanceller implements Listener, ConfigHandler.Extensions {

  private static final ItemTag<Boolean> ITEM_META = ItemTag.newBoolean("cancelled-drop");

  @Getter private final ConfigHandler config = new ConfigHandler();
  private final Supplier<Set<Material>> MATERIAL_LIST =
      useConfig("material-list", Set.of(), MATERIAL_SET_READER);

  private @Nullable WoolMatchModule woolMatchModule;

  public ItemRemoveCanceller(Bingo bingo) {
    Bukkit.getServer().getPluginManager().registerEvents(this, bingo);
  }

  @Override
  public String getConfigSection() {
    return "item-remove-canceller";
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void processItemRemoval(ItemSpawnEvent event) {
    if (!event.isCancelled()) return;

    ItemStack item = event.getEntity().getItemStack();
    if (MATERIAL_LIST.get().contains(item.getType())) {
      event.setCancelled(false);
      applyCustomMeta(item);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onItemCraft(PrepareItemCraftEvent event) {
    // Check if any items in the ingredient list are cancelled drops
    boolean metaCrafted =
        Arrays.stream(event.getInventory().getContents()).anyMatch(ITEM_META::has);
    if (!metaCrafted) return;

    ItemStack result = event.getRecipe().getResult();
    if (result == null) return;

    // If trying to craft a wool type cancel
    if (result.getType().equals(Material.WOOL)) {
      if (!canCraftWool(result.getData().getData())) {
        event.getInventory().setResult(null);
        return;
      }
    }

    ITEM_META.set(result, true);
    event.getInventory().setResult(result);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    woolMatchModule = event.getMatch().getModule(WoolMatchModule.class);
  }

  public static void applyCustomMeta(ItemStack item) {
    if (ITEM_META.has(item)) return;

    ITEM_META.set(item, true);
  }

  public boolean canCraftWool(byte woolColor) {
    if (woolMatchModule == null) return true;

    BitSet woolColours =
        woolMatchModule.getWools().values().stream()
            .filter(monumentWool -> !monumentWool.isCompleted())
            .map(monumentWool -> monumentWool.getDyeColor().getWoolData())
            .collect(() -> new BitSet(16), BitSet::set, BitSet::or);

    if (woolColours.isEmpty()) return true;

    return !woolColours.get(woolColor);
  }
}
