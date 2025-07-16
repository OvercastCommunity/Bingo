package tc.oc.bingo.modules;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.ConfigHandler;
import tc.oc.bingo.config.ConfigReader;
import tc.oc.bingo.util.CustomItem;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.material.MaterialData;

public class CustomItemModule implements ManagedListener, ConfigHandler.Extensions {

  public static final CustomItemModule INSTANCE = new CustomItemModule();

  private static final CustomItem UNKNOWN_ITEM = new CustomItem("unknown", "Unknown", null, "");

  private static final ConfigReader<CustomItem> CUSTOM_ITEM_READER =
      (cfg, key, def) -> {
        ConfigurationSection configurationSection = cfg.getConfigurationSection(key);
        return new CustomItem(
            key,
            configurationSection.getString("name", key),
            configurationSection.getStringList("lore"),
            configurationSection.getString("texture", def.texture()));
      };

  public static final ItemTag<String> CUSTOM_ITEM_META = ItemTag.newString("custom-item-id");

  private final Map<String, Supplier<CustomItem>> customItems = new HashMap<>();

  @Getter private final ConfigHandler config = new ConfigHandler();

  private final Random random = new Random();

  private CustomItemModule() {}

  public Supplier<CustomItem> getItem(String id) {
    return customItems.computeIfAbsent(
        id.toLowerCase(Locale.ROOT), key -> useConfig(key, UNKNOWN_ITEM, CUSTOM_ITEM_READER));
  }

  public static String getItemId(ItemStack item) {
    return CUSTOM_ITEM_META.get(item);
  }

  public static boolean isCustomItem(ItemStack item, Supplier<CustomItem> customItemSupplier) {
    return Objects.equals(CUSTOM_ITEM_META.get(item), customItemSupplier.get().id());
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();
    if (block.getType() != Material.SKULL) return;

    ItemStack item = event.getItemInHand();
    String itemId = getItemId(item);
    if (itemId == null) return;

    block.setMetadata("custom-item-id", new FixedMetadataValue(Bingo.get(), itemId));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockTransformEvent event) {
    if (!event.getOldState().getType().equals(Material.SKULL)) return;

    Block block = event.getBlock();
    if (block.getType() != Material.SKULL) return;

    MetadataValue metadata = block.getMetadata("custom-item-id", Bingo.get());
    if (metadata == null) return;

    String customItemId = metadata.asString();

    event.setCancelled(true);
    final BlockState newState = event.getNewState();

    MaterialData.block(newState).applyTo(block, true);

    Location dropLocation = block.getLocation().clone();
    dropLocation.setX(dropLocation.getBlockX() + random.nextDouble() * 0.5 + 0.25);
    dropLocation.setY(dropLocation.getBlockY() + random.nextDouble() * 0.5 + 0.25);
    dropLocation.setZ(dropLocation.getBlockZ() + random.nextDouble() * 0.5 + 0.25);
    dropLocation.getWorld().dropItem(dropLocation, getItem(customItemId).get().toItemStack());
  }

  @Override
  public String getConfigSection() {
    return "custom-items";
  }
}
