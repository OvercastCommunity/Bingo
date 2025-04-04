package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.killreward.KillRewardMatchModule;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.nms.NMSHacks;

@Tracker("care-package")
public class CarePackageObjective extends ObjectiveTracker {

  // When player get a 5 kill streak (configable)

  private static final ItemTag<Boolean> EGG_ITEM = ItemTag.newBoolean("custom-egg-item");
  public static final String EGG_META = "care-package-egg";
  private static final MetadataValue eggMetaValue =
      MetadataUtils.createMetadataValue(Bingo.get(), true);

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
  }

  @EventHandler
  public void onParticipantKitApplyEvent(ParticipantKitApplyEvent event) {
    ItemStack itemStack = new ItemStack(Material.MONSTER_EGG, 1, (short) 93);
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(ChatColor.RESET + "Care Package Spawn Egg");
    itemStack.setItemMeta(itemMeta);
    EGG_ITEM.set(itemStack, true);

    event.getPlayer().getBukkit().getInventory().addItem(itemStack);
  }

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer matchPlayer = getPlayer(event.getKiller());
    if (matchPlayer == null) return;

    int streak = killRewardModule.getKillStreak(matchPlayer.getId());
    if (streak != REQUIRED_STREAK.get()) return;

    // Create item with name and meta
    ItemStack itemStack = new ItemStack(Material.MONSTER_EGG, 1, (short) 93);
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(ChatColor.RESET + "Care Package Spawn Egg");
    itemStack.setItemMeta(itemMeta);
    EGG_ITEM.set(itemStack, true);

    matchPlayer.getBukkit().getInventory().addItem(itemStack);
    // TODO: what if player inventory full

    // matchPlayer.getBukkit().getInventory().getContents()

    // Check if they have a 5 kill streak
    // If so, give them a care package item
    // Maybe give them a care package item every `x` kills?
  }

  @EventHandler(ignoreCancelled = true)
  public void onChestOpen(PlayerInteractEvent event) {
    // Detect player opening a chest
    if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
        && event.getClickedBlock().getType().equals(Material.CHEST)) {

      // Check if the chest has metadata
      if (!event.getClickedBlock().getState().hasMetadata(EGG_META)) return;

      MatchPlayer matchPlayer = getPlayer(event.getPlayer());
      if (matchPlayer == null || !matchPlayer.isParticipating()) return;

      // Check that the player is the owner
      liveCarePackages.stream()
          .filter(
              carePackage -> {
                if (carePackage.placedChest == null) return false;

                return carePackage.placedChest.equals(event.getClickedBlock());
              })
          .findFirst()
          .ifPresent(
              carePackage -> {
                // Check carePackage.placedAt against current time and CHEST_LOCKED_SECONDS
                // Check if the player is the owner
                boolean isOwner = carePackage.owner.equals(matchPlayer.getId());
                boolean isLocked =
                    System.currentTimeMillis() - carePackage.landedAt
                        < CHEST_LOCKED_SECONDS.get() * 1000;

                MatchPlayer ownerPlayer = getPlayer(carePackage.owner);

                if (!isOwner && isLocked && ownerPlayer != null) {
                  matchPlayer.sendMessage(
                      text("This care package belongs to ", NamedTextColor.RED)
                          .append(ownerPlayer.getName(NameStyle.SIMPLE_COLOR))
                          .append(text("!", NamedTextColor.RED)));
                  matchPlayer
                      .getBukkit()
                      .playSound(carePackage.placedChest.getLocation(), Sound.DOOR_OPEN, 1f, 1f);
                  event.setCancelled(true);
                  return;
                }

                if (isOwner) {
                  reward(matchPlayer.getBukkit());
                }

                carePackage.placedChest.setType(Material.AIR);
                liveCarePackages.remove(carePackage);

                Location centerLocation = carePackage.placedChest.getLocation().toCenterLocation();
                matchPlayer
                    .getBukkit()
                    .getWorld()
                    .playSound(centerLocation, Sound.CHEST_OPEN, 1f, 1f);

                ItemStack loot;

                int choice = (int) (Math.random() * 6);
                switch (choice) {
                  case 0 -> loot = new ItemStack(Material.MILK_BUCKET, 1);
                  case 1 -> loot = new ItemStack(Material.GOLDEN_APPLE, 1);
                  case 2 -> loot = new ItemStack(Material.DIAMOND, 1);
                  case 3 -> loot = new ItemStack(Material.CACTUS, 1);
                  case 4 -> loot = new ItemStack(Material.EXP_BOTTLE, 3);
                  case 5 -> loot = new ItemStack(Material.BREAD, 5);
                  default -> loot = new ItemStack(Material.GRASS, 1);
                }

                event.getWorld().dropItemNaturally(centerLocation, loot);
              });
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

    if (event.getItem() == null) return;

    if (!EGG_ITEM.has(event.getItem())) return;
    event.setCancelled(true);

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null) return;

    // Check the player clicked the top face of a block
    if (!event.getBlockFace().equals(BlockFace.UP)) {
      matchPlayer.sendMessage(
          text(
              "You must click the top face of a block to spawn a care package!",
              NamedTextColor.RED));
      return;
    }

    // Check that blocks from clicked block SPAWN_HEIGHT blocks are all air
    Block clickedBlock = event.getClickedBlock();
    int startingY = clickedBlock.getY() + 1;
    for (int i = startingY; i < startingY + SPAWN_HEIGHT.get(); i++) {
      Block block = clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), i, clickedBlock.getZ());
      if (!block.getType().equals(Material.AIR)) {
        matchPlayer.sendMessage(
            text(
                "You must click a block with air above it to spawn a care package!",
                NamedTextColor.RED));
        return;
      }
    }

    // Remove a spawn egg from players hand
    ItemStack itemInHand = event.getPlayer().getItemInHand();
    itemInHand.setAmount(itemInHand.getAmount() - 1);
    event.getPlayer().setItemInHand(itemInHand);

    triggerCarePackage(event.getPlayer(), clickedBlock, SPAWN_HEIGHT.get());
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamageEvent(EntityDamageEvent event) {
    // Prevent damage to fake mobs
    if (event.getEntity().hasMetadata(EGG_META)) {
      event.setCancelled(true);
    }
  }

  private void triggerCarePackage(Player player, Block clickedBlock, Integer spawnHeight) {
    CarePackage carePackage = new CarePackage(player);
    liveCarePackages.add(carePackage);

    carePackage.spawn(
        clickedBlock.getLocation().add(0.5, spawnHeight, 0.5),
        yawToFace(player.getLocation().getYaw()));
  }

  // Give them an item that allows them to summon a care package

  // Maybe once player places it on ground it will take 10 seconds to drop (check they can build?
  // before giving them?)

  // Care package will drop from the sky (falling chest with chickens on leads attached like a
  // parachute)

  // When chest lands the person who called it in can open and get some set loot

  // Maybe remove after `x` time of not being opened?
  // Only allow opener to use it? locked for just them for `x` seconds?
  // Remove chest once looted?

  // Reward player once they take out the loot (or open the chest)

  public class CarePackage {

    public final UUID owner;

    public LivingEntity zombie;
    public LivingEntity leashHolder;
    public LivingEntity chicken;

    public BlockFace placeFace;
    public Block placedChest;
    public long landedAt;

    BukkitTask movementTask;

    private CarePackage(Player player) {
      this.owner = player.getUniqueId();
    }

    public void spawn(Location spawnLocation, BlockFace spawnDirection) {

      this.placeFace = spawnDirection.getOppositeFace();
      World world = spawnLocation.getWorld();

      Zombie zombie = (Zombie) world.spawnEntity(spawnLocation, EntityType.ZOMBIE);
      zombie.setCanPickupItems(false);
      zombie.getEquipment().setChestplate(new ItemStack(Material.AIR));
      zombie.getEquipment().setLeggings(new ItemStack(Material.AIR));
      zombie.getEquipment().setBoots(new ItemStack(Material.AIR));
      zombie.setBaby(false);

      PotionEffect invisibility =
          new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false);
      zombie.addPotionEffect(invisibility);

      Location zombieDirection =
          zombie
              .getLocation()
              .setDirection(
                  new Vector(
                      placeFace.getModX() * 90,
                      placeFace.getModY() * 90,
                      placeFace.getModZ() * 90));

      zombie.teleport(zombieDirection);

      NMSHacks.NMS_HACKS.freezeEntity(zombie);

      // Set chest on its head
      zombie.getEquipment().setHelmet(new ItemStack(Material.CHEST));
      zombie.setMetadata(EGG_META, eggMetaValue);

      // new Vector(placeFace.getModX() * 90, placeFace.getModY() * 90, placeFace.getModZ() * 90));

      LivingEntity leashPoint =
          (LivingEntity)
              world.spawnEntity(spawnLocation.clone().add(0.7, -3, -0.5), EntityType.CHICKEN);
      NMSHacks.NMS_HACKS.freezeEntity(leashPoint);
      leashPoint.addPotionEffect(invisibility);
      leashPoint.setMetadata(EGG_META, eggMetaValue);

      // Spawn the chicken above
      Location chickenLocation = spawnLocation.clone().add(0, 3, 0);
      Chicken chicken = (Chicken) world.spawnEntity(chickenLocation, EntityType.CHICKEN);

      List<Chicken> toLeash = new ArrayList<>();
      toLeash.add(chicken);
      for (int i = 0; i < 3 + (int) (Math.random() * 3); i++) {
        Location nextLocation =
            chicken
                .getLocation()
                .clone()
                .add(
                    (Math.random() - 0.5) * 1.5,
                    1 + Math.random() * 1.2,
                    (Math.random() - 0.5) * 1.5);
        toLeash.add((Chicken) world.spawnEntity(nextLocation, EntityType.CHICKEN));
      }

      this.zombie = zombie;
      this.leashHolder = leashPoint;
      this.chicken = chicken;

      CarePackage carePackage = this;

      // Lead the chicken to the zombie
      new BukkitRunnable() {
        @Override
        public void run() {
          if (!carePackage.isValid()) {
            this.cancel();
            return;
          }
          toLeash.forEach(
              chick -> {
                chick.setLeashHolder(leashPoint);
              });
        }
      }.runTaskLater(Bingo.get(), 1L);

      // Sync zombie position with chicken, maintaining 5 block distance
      movementTask =
          new BukkitRunnable() {
            @Override
            public void run() {
              if (!carePackage.isValid()) {
                this.cancel();
                return;
              }

              if (!carePackage.updateLocation()) {
                this.cancel();
              }
            }
          }.runTaskTimer(Bingo.get(), 0L, 2L); // Repeats every 2 ticks
    }

    public boolean isValid() {
      return zombie.isValid() && leashHolder.isValid();
    }

    public boolean updateLocation() {

      // When the chicken is alive move based on the chicken
      if (chicken.isValid()) {
        Location chickenLoc = chicken.getLocation();
        Location newZombieLoc = chickenLoc.clone().add(0, -5, 0);
        zombie.teleport(newZombieLoc);

        // leashHolder.setLeashHolder(chicken);

        Location newLeashLocation = chickenLoc.clone().add(0.7, -3, -0.5);
        leashHolder.teleport(newLeashLocation);
      } else {
        // Move the zombie on its own
        zombie.teleport(zombie.getLocation().subtract(0, 0.4, 0));
      }

      // Check if the zombie is near the ground
      Location checkLocation = zombie.getLocation().add(0, 1, 0);
      boolean isGrounded = !checkLocation.getBlock().isEmpty();
      boolean isVoided = checkLocation.getY() < 0;

      if (isGrounded || isVoided) {
        movementTask.cancel();

        if (!isVoided) {
          Location chestLocation = checkLocation.add(0, 1, 0);
          Block blockAt = zombie.getWorld().getBlockAt(chestLocation);
          blockAt.setType(Material.CHEST);

          // Set the direction and store the chest
          Block modifiedBlock = zombie.getWorld().getBlockAt(chestLocation);
          if (modifiedBlock.getState() instanceof Chest chest) {
            MaterialData materialData = chest.getMaterialData();
            if (materialData instanceof Directional directional) {
              directional.setFacingDirection(placeFace);
              placedChest = modifiedBlock;
              chest.update();
              chest.setMetadata(EGG_META, eggMetaValue);
            }
          }

          // Start timer to remove after CHEST_ALIVE_SECONDS
          new BukkitRunnable() {
            @Override
            public void run() {
              if (placedChest.getType().equals(Material.CHEST)) {
                placedChest.setType(Material.AIR);
                liveCarePackages.remove(CarePackage.this);
                placedChest
                    .getLocation()
                    .getWorld()
                    .playEffect(
                        placedChest.getLocation().add(0.5, 0.5, 0.5), Effect.EXPLOSION_LARGE, 1);
              }
            }
          }.runTaskLater(Bingo.get(), CHEST_ALIVE_SECONDS.get() * 20L);
        } else {
          liveCarePackages.remove(this);
        }

        landedAt = System.currentTimeMillis();
        zombie.remove();
        leashHolder.remove();

        return false;
      }

      return true;
    }
  }

  private static BlockFace yawToFace(float yaw) {
    return CLOCKWISE[Math.round(((yaw + 360) % 360) / 90f) & 0x3];
  }
}
