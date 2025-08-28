package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;

@Tracker("map-making")
public class MapMakingObjective extends ObjectiveTracker.Stateful<Integer> {

  private static final Random RANDOM = new Random();

  private final Map<Integer, Short> mapCrafts = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    // mapCrafts.clear();
  }

  @EventHandler(ignoreCancelled = true)
  public void onMapCraft(CraftItemEvent event) {
    if (event.getRecipe() == null || event.getRecipe().getResult().getType() != Material.EMPTY_MAP)
      return;

    // Ensure the player is crafting the base map
    ItemStack crafted = event.getCurrentItem();
    if (crafted == null || crafted.getType() != Material.EMPTY_MAP) return;

    // Roll: 1 = 1st, 2 = 2nd, 3 = 3rd
    int roll = RANDOM.nextInt(100);
    int place = (roll < 20) ? 1 : (roll < 55) ? 2 : 3;
    Player player = event.getActor();

    // Every 3rd crafted map is a 1st place map guaranteed
    if ((updateObjectiveData(player.getUniqueId(), integer -> integer + 1) % 3) == 0) {
      place = 1;
    }

    String title =
        switch (place) {
          case 1 -> ChatColor.GOLD + "1st Place Map";
          case 2 -> ChatColor.GRAY + "2nd Place Map";
          case 3 -> ChatColor.DARK_RED + "3rd Place Map";
          default -> ChatColor.WHITE + "Unknown Map";
        };

    // Create a custom map render for the item
    short positionMap = getMapId(player, place);
    ItemStack mapItem = new ItemStack(Material.MAP, 1);
    mapItem.setDurability(positionMap);
    MapMeta meta = (MapMeta) mapItem.getItemMeta();
    meta.setDisplayName(title);
    mapItem.setItemMeta(meta);

    // Apply changes directly to result
    event.setCurrentItem(mapItem);

    // Reward only if 1st place
    if (place == 1) {
      reward(player);
    }
  }

  private short getMapId(Player player, int place) {
    if (mapCrafts.get(place) != null) {
      return mapCrafts.get(place);
    }

    MapView mapView = Bukkit.createMap(player.getWorld());
    short id = mapView.getId();

    mapView.getRenderers().clear();

    mapView.addRenderer(
        new MapRenderer() {
          boolean rendered = false;

          @Override
          public void render(MapView view, MapCanvas canvas, Player renderingPlayer) {
            // Only render once
            if (rendered) return;

            StringBuilder stringBuilder =
                (new StringBuilder())
                    .append("You crafted a map..\n\n")
                    .append("§" + MapPalette.BLUE + ";")
                    .append("So time for a map\n")
                    .append("§" + MapPalette.BLUE + ";")
                    .append("making contest.\n\n")
                    .append("§" + MapPalette.BLUE + ";")
                    .append("Our judges have..\n")
                    .append("§" + MapPalette.BLUE + ";")
                    .append("judged and awarded\n")
                    .append("§" + MapPalette.BLUE + ";")
                    .append("you with...");

            canvas.drawText(12, 12, MinecraftFont.Font, stringBuilder.toString());

            canvas.drawText(
                30,
                100,
                MinecraftFont.Font,
                "§"
                    + MapPalette.DARK_GREEN
                    + ";"
                    + switch (place) {
                      case 1 -> "FIRST PLACE!";
                      case 2 -> "SECOND PLACE!";
                      case 3 -> "THIRD PLACE!";
                      default -> "";
                    });

            rendered = true;
          }
        });

    mapCrafts.put(place, id);
    return id;
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return 0;
  }

  @Override
  public Double getProgress(UUID uuid) {
    return null;
  }
}
