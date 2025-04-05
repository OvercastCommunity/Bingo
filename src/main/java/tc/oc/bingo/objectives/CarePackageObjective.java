package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collector;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.killreward.KillRewardMatchModule;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.nms.NMSHacks;

@Tracker("care-package")
public class CarePackageObjective extends ObjectiveTracker {

  private static final ItemTag<Boolean> EGG_ITEM = ItemTag.newBoolean("custom-egg-item");
  private static final String EGG_META = "care-package-egg";
  private static final MetadataValue eggMetaValue =
      MetadataUtils.createMetadataValue(Bingo.get(), true);

  private static final Component TOP_FACE =
      text("You must click the top face of a block to spawn a care package!");
  private static final Component OPEN_AIR =
      text("You must click a block with air above it to spawn a care package!");

  private final Supplier<Integer> REQUIRED_STREAK = useConfig("required-streak", 5);
  private final Supplier<Integer> SPAWN_HEIGHT = useConfig("spawn-height", 30);

  private final Supplier<Integer> CHEST_LOCKED_SECONDS = useConfig("chest-locked-seconds", 10);
  private final Supplier<Integer> CHEST_ALIVE_SECONDS = useConfig("chest-alive-seconds", 30);

  private KillRewardMatchModule killRewardModule;

  private final List<CarePackage> liveCarePackages = new ArrayList<>();

  static BlockFace[] CLOCKWISE =
      new BlockFace[] {
        BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST,
      };

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    killRewardModule = event.getMatch().getModule(KillRewardMatchModule.class);
    liveCarePackages.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchEnd(MatchFinishEvent event) {
    // Deleting all associated entities will also cause any ongoing task to end
    for (CarePackage carePackage : liveCarePackages) carePackage.deleteEntities();
    liveCarePackages.clear();
  }

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer matchPlayer = getPlayer(event.getKiller());
    if (matchPlayer == null) return;

    int streak = killRewardModule.getKillStreak(matchPlayer.getId());
    if (streak != REQUIRED_STREAK.get()) return;

    matchPlayer.getBukkit().getInventory().addItem(carePackageItem());

    // Check if they have a 5 kill streak
    // If so, give them a care package item
    // Maybe give them a care package item every `x` kills?
  }

  private ItemStack carePackageItem() {
    ItemStack itemStack = new ItemStack(Material.MONSTER_EGG, 1, (short) 93);
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(ChatColor.RESET + "Care Package Spawn Egg");
    itemStack.setItemMeta(itemMeta);
    EGG_ITEM.set(itemStack, true);
    return itemStack;
  }

  @EventHandler(ignoreCancelled = true)
  public void onChestOpen(PlayerInteractEvent event) {
    // Detect player opening a chest
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
    if (!event.getClickedBlock().getType().equals(Material.CHEST)) return;
    if (!event.getClickedBlock().getState().hasMetadata(EGG_META)) return;

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (!MatchPlayers.canInteract(matchPlayer)) return;

    // Check that the player is the owner
    var carePackage =
        liveCarePackages.stream()
            .filter(p -> p.placedChest != null && p.placedChest.equals(event.getClickedBlock()))
            .findFirst()
            .orElse(null);
    if (carePackage == null) return;

    // Check carePackage.placedAt against current time and CHEST_LOCKED_SECONDS
    // Check if the player is the owner
    boolean isOwner = carePackage.owner.equals(matchPlayer.getId());
    boolean isLocked =
        System.currentTimeMillis() - carePackage.landedAt < CHEST_LOCKED_SECONDS.get() * 1000;

    MatchPlayer ownerPlayer = getPlayer(carePackage.owner);

    if (!isOwner && isLocked && ownerPlayer != null) {
      matchPlayer.sendWarning(
          text("This care package belongs to ")
              .append(ownerPlayer.getName(NameStyle.SIMPLE_COLOR))
              .append(text("!")));
      matchPlayer
          .getBukkit()
          .playSound(carePackage.placedChest.getLocation(), Sound.DOOR_OPEN, 1f, 1f);
      event.setCancelled(true);
      return;
    }

    if (isOwner) reward(matchPlayer.getBukkit());

    carePackage.placedChest.setType(Material.AIR);
    carePackage.chickens.forEach(Entity::remove);
    liveCarePackages.remove(carePackage);

    Location centerLocation = carePackage.placedChest.getLocation().toCenterLocation();
    matchPlayer.getBukkit().getWorld().playSound(centerLocation, Sound.CHEST_OPEN, 1f, 1f);

    ItemStack loot =
        switch ((int) (Math.random() * 7)) {
          case 0 -> new ItemStack(Material.MILK_BUCKET, 1);
          case 1 -> new ItemStack(Material.GOLDEN_APPLE, 1);
          case 2 -> new ItemStack(Material.DIAMOND, 1);
          case 3 -> new ItemStack(Material.CACTUS, 1);
          case 4 -> new ItemStack(Material.EXP_BOTTLE, 3);
          case 5 -> new ItemStack(Material.BREAD, 5);
          default -> new ItemStack(Material.GRASS, 1);
        };

    event.getWorld().dropItemNaturally(centerLocation, loot);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
    var item = event.getItem();
    if (item == null || !EGG_ITEM.has(event.getItem())) return;
    event.setCancelled(true);

    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    // Check the player clicked the top face of a block
    if (!event.getBlockFace().equals(BlockFace.UP)) {
      player.sendWarning(TOP_FACE);
      return;
    }

    // Check that blocks from clicked block SPAWN_HEIGHT blocks are all air
    Block clickedBlock = event.getClickedBlock();
    for (int i = clickedBlock.getY() + SPAWN_HEIGHT.get(); i > clickedBlock.getY(); i--) {
      Block block = clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), i, clickedBlock.getZ());
      if (!block.getType().equals(Material.AIR)) {
        player.sendWarning(OPEN_AIR);
        return;
      }
    }

    // Remove a spawn egg from players hand
    InventoryUtils.consumeItem(event);
    liveCarePackages.add(
        new CarePackage(
            event.getPlayer(),
            clickedBlock.getLocation().add(0.5, SPAWN_HEIGHT.get(), 0.5),
            yawToFace(player.getLocation().getYaw())));
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamageEvent(EntityDamageEvent event) {
    // Prevent damage to fake mobs
    if (event.getEntity().hasMetadata(EGG_META)) event.setCancelled(true);
  }

  public class CarePackage {
    private static final ScheduledExecutorService EXECUTOR = PGM.get().getExecutor();
    private final UUID owner;
    private final ArmorStand chest;
    private final ArmorStand leasher;
    private final List<Chicken> chickens;
    private final Future<?> movementTask;

    private final BlockFace placeFace;
    private Block placedChest;
    private long landedAt;

    private CarePackage(Player player, Location spawnAt, BlockFace facing) {
      this.owner = player.getUniqueId();
      this.placeFace = facing.getOppositeFace();

      World world = spawnAt.getWorld();
      spawnAt =
          spawnAt
              .clone()
              .setDirection(
                  new Vector(
                      placeFace.getModX() * 90,
                      placeFace.getModY() * 90,
                      placeFace.getModZ() * 90));
      chest = world.spawn(spawnAt, ArmorStand.class);
      chest.setVisible(false);
      chest.setBasePlate(false);
      NMSHacks.NMS_HACKS.freezeEntity(chest);
      chest.getEquipment().setHelmet(new ItemStack(Material.CHEST));
      chest.setMetadata(EGG_META, eggMetaValue);

      leasher = world.spawn(spawnAt, ArmorStand.class);
      leasher.setVisible(false);
      leasher.setBasePlate(false);
      NMSHacks.NMS_HACKS.freezeEntity(leasher);
      leasher.setMetadata(EGG_META, eggMetaValue);

      // Spawn the chicken above
      chickens = new ArrayList<>();
      for (int i = 0; i < 3 + (int) (Math.random() * 3); i++) {
        var loc = spawnAt.clone();
        loc.add((Math.random() - 0.5) * 1.5, 4 + Math.random() * 1.2, (Math.random() - 0.5) * 1.5);
        chickens.add(world.spawn(loc, Chicken.class));
      }

      // Leash the chicken to the zombie
      EXECUTOR.schedule(
          () -> {
            if (CarePackage.this.isValid())
              chickens.forEach(chick -> chick.setLeashHolder(leasher));
          },
          50,
          TimeUnit.MILLISECONDS);

      // Sync zombie position with chicken, maintaining 5 block distance
      movementTask = EXECUTOR.scheduleAtFixedRate(this::updateTask, 0, 100, TimeUnit.MILLISECONDS);
    }

    public boolean isValid() {
      return chest.isValid();
    }

    private void updateTask() {
      if (!isValid() || !updateLocation()) {
        movementTask.cancel(false);
      }
    }

    // True when it continues moving, false when it has landed or voided
    public boolean updateLocation() {
      // When chickens are alive, use avg pos of them
      Vector hangPoint =
          chickens.stream()
              .filter(Entity::isValid)
              .map(Entity::getLocation)
              .collect(averagingPosition());
      // If not, use chest +4.6 as hang point
      if (hangPoint == null) {
        hangPoint = chest.getLocation().toVector();
        hangPoint.setY(hangPoint.getY() + 4.6);
      }

      Location loc = chest.getLocation();
      ((CraftEntity) chest)
          .getHandle()
          .move(
              Math.clamp(hangPoint.getX() - loc.getX(), -0.2d, 0.2d),
              Math.clamp(hangPoint.getY() - 5 - loc.getY(), -0.8d, -0.05d),
              Math.clamp(hangPoint.getZ() - loc.getZ(), -0.2d, 0.2d));
      // Leash relative offset for 1.8 players.
      // Newer versions will see it point to a wrong position, but we can't do much about it.
      Location leashLoc = chest.getLocation().add(-0.7, 1, 0.5);
      leashLoc.setYaw(180f);
      leasher.teleport(leashLoc);

      // Check if the zombie is near the ground
      Location checkLocation = chest.getLocation().add(0, 1, 0);
      boolean isGrounded = !checkLocation.getBlock().isEmpty();
      boolean isVoided = checkLocation.getY() < 0;

      if (!isGrounded && !isVoided) return true;

      if (!isVoided) {
        Location chestLocation = checkLocation.add(0, 1, 0);
        Block blockAt = chest.getWorld().getBlockAt(chestLocation);
        blockAt.setType(Material.CHEST);

        // Set the direction and store the chest
        Block modifiedBlock = chest.getWorld().getBlockAt(chestLocation);
        if (modifiedBlock.getState() instanceof Chest block) {
          MaterialData materialData = block.getMaterialData();
          if (materialData instanceof Directional directional) {
            directional.setFacingDirection(placeFace);
            placedChest = modifiedBlock;
            block.update();
            block.setMetadata(EGG_META, eggMetaValue);
          }
        }

        // Start timer to remove after CHEST_ALIVE_SECONDS
        EXECUTOR.schedule(
            () -> {
              chickens.forEach(Entity::remove);
              if (!placedChest.getType().equals(Material.CHEST)) return;
              placedChest.setType(Material.AIR);
              liveCarePackages.remove(CarePackage.this);
              placedChest
                  .getLocation()
                  .getWorld()
                  .playEffect(
                      placedChest.getLocation().add(0.5, 0.5, 0.5), Effect.EXPLOSION_LARGE, 1);
            },
            CHEST_ALIVE_SECONDS.get(),
            TimeUnit.SECONDS);

        landedAt = System.currentTimeMillis();
        leasher.remove();
        chest.remove();
      } else {
        liveCarePackages.remove(this);
        deleteEntities();
      }
      return false;
    }

    private void deleteEntities() {
      leasher.remove();
      chest.remove();
      chickens.forEach(Entity::remove);
    }
  }

  private static BlockFace yawToFace(float yaw) {
    return CLOCKWISE[Math.round(((yaw + 360) % 360) / 90f) & 0x3];
  }

  private static Collector<Location, ?, Vector> averagingPosition() {
    // Calculates an average vector for a set of Location
    return Collector.of(
        () -> new double[4],
        (a, t) -> {
          a[0] += t.getX();
          a[1] += t.getY();
          a[2] += t.getZ();
          a[3]++;
        },
        (a, b) -> {
          a[0] += b[0];
          a[1] += b[1];
          a[2] += b[2];
          a[3] += b[4];
          return a;
        },
        a -> a[3] == 0 ? null : new Vector(a[0] / a[3], a[1] / a[3], a[2] / a[3]));
  }
}
