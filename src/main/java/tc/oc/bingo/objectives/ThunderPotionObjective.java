package tc.oc.bingo.objectives;

import static tc.oc.bingo.modules.ItemRemoveCanceller.ITEM_META;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomPotionsModule;

@Tracker("thunder-potion-task")
public class ThunderPotionObjective extends ObjectiveTracker {

  private final Supplier<Double> SPAWN_CHANCE = useConfig("skeleton-horse-chance", 0.35d);

  @Override
  public void setupDependencies() {
    CustomPotionsModule.CustomPotionType customPotionType =
        new CustomPotionsModule.CustomPotionType(
            "thunder", this::checkIngredient, this::createPotion);

    CustomPotionsModule.INSTANCE.registerPotion("thunder", customPotionType);
  }

  @Override
  public void teardownDependencies() {
    CustomPotionsModule.INSTANCE.removePotion("thunder");
  }

  private Boolean checkIngredient(ItemStack itemStack) {
    // Require blaze powder to brew
    if (!itemStack.getType().equals(Material.BLAZE_POWDER)) return false;

    // Require item to be a custom item
    return ITEM_META.has(itemStack);
  }

  private ItemStack createPotion() {
    return CustomPotionsModule.createPotion(
        "§ePotion of Thunder", List.of("§cInstant Thunder", "§7Unleash the storm..."), (short) 9);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerConsumePotion(CustomPotionsModule.CustomPotionDrinkEvent event) {
    Player player = event.getPlayer();

    if (!event.getSlug().equals("thunder")) return;
    if (!CustomPotionsModule.hasEffect(player, "thunder")) return;

    World world = player.getWorld();

    // Always strike lightning at the player
    // TODO: maybe just effect?
    world.strikeLightning(player.getLocation());

    // Skeleton horse spawn chance
    if (Math.random() <= SPAWN_CHANCE.get()) {
      spawnHorse(player.getLocation(), player);

      // Reward the player only if skeleton horse spawns
      reward(player);
    }
  }

  public void spawnHorse(Location loc, Player player) {
    CraftWorld craftWorld = (CraftWorld) loc.getWorld();
    Entity nmsHorse = craftWorld.createEntity(loc, Horse.class);
    if (nmsHorse instanceof EntityInsentient ei)
      ei.prepare(craftWorld.getHandle().E(new BlockPosition(ei)), null);

    Horse horse = (Horse) nmsHorse.getBukkitEntity();
    horse.setVariant(Horse.Variant.SKELETON_HORSE);
    horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
    horse.setAdult();
    horse.setTamed(true);
    horse.setMaxHealth(20);
    horse.setOwner(player);

    craftWorld.addEntity(nmsHorse, CreatureSpawnEvent.SpawnReason.CUSTOM);
  }
}
