package tc.oc.bingo.listeners;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Set;
import java.util.function.Supplier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.ConfigHandler;

public class ItemRemoveCanceller implements Listener, ConfigHandler.Extensions {

  @Getter private final ConfigHandler config = new ConfigHandler();
  private final Supplier<Set<Material>> MATERIAL_LIST =
      useConfig("material-list", Set.of(), MATERIAL_SET_READER);

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
    }
  }
}
