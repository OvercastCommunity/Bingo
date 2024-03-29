package tc.oc.bingo.util;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public class LocationUtils {

  public static boolean stoodInMaterial(Location location, Material material) {
    // Player hit-box is 0.6 by 1.8
    double minX = Math.floor(location.getX() - 0.3);
    double maxX = Math.ceil(location.getX() + 0.3);
    double minZ = Math.floor(location.getZ() - 0.3);
    double maxZ = Math.ceil(location.getZ() + 0.3);
    double minY = Math.floor(location.getY());
    double maxY = Math.ceil(location.getY() + 1.8);

    for (double x = minX; x < maxX; x++) {
      for (double z = minZ; z < maxZ; z++) {
        for (double y = minY; y < maxY; y++) {
          Block block = location.getWorld().getBlockAt((int) x, (int) y, (int) z);
          if (block != null && block.getType() == material) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean stoodInBlock(Location location, Vector vector) {
    // Player hit-box is 0.6 by 1.8
    double minX = Math.floor(location.getX() - 0.3);
    double maxX = Math.ceil(location.getX() + 0.3);
    double minZ = Math.floor(location.getZ() - 0.3);
    double maxZ = Math.ceil(location.getZ() + 0.3);
    double minY = Math.floor(location.getY());
    double maxY = Math.ceil(location.getY() + 1.8);

    Location vectorLocation = vector.toLocation(location.getWorld());

    for (double x = minX; x < maxX; x++) {
      for (double z = minZ; z < maxZ; z++) {
        for (double y = minY; y < maxY; y++) {
          Block block = location.getWorld().getBlockAt((int) x, (int) y, (int) z);
          if (block.getLocation().equals(vectorLocation)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static void spawnFirework(Location location, FireworkEffect effect, int power) {
    FireworkMeta meta = (FireworkMeta) Bukkit.getItemFactory().getItemMeta(Material.FIREWORK);
    meta.setPower(power);
    meta.addEffect(effect);

    Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
    firework.setFireworkMeta(meta);
  }
}
