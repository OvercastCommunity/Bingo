package tc.oc.bingo.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.objectives.Scope;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.nms.NMSHacks;

@BingoModule.Config("graves")
public class GravesModule extends BingoModule {

  public static final GravesModule INSTANCE = new GravesModule();
  public static final String GRAVE_META = "halloween-grave";

  private final Supplier<Double> GRAVE_CHANCE = useConfig("grave-chance", 0.25d);
  private final Supplier<Integer> GRAVE_ALIVE_SECONDS = useConfig("grave-alive-seconds", 30);
  private final Supplier<Integer> DOWNWARDS_ALLOWANCE = useConfig("downwards-place-allowance", 2);

  private final Map<UUID, Grave> activeGraves = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    activeGraves.values().forEach(Grave::remove);
    activeGraves.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Grave grave = activeGraves.remove(event.getPlayer().getUniqueId());
    if (grave != null) {
      grave.remove();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamChange(PlayerPartyChangeEvent event) {
    Grave grave = activeGraves.remove(event.getPlayer().getId());
    if (grave != null) {
      grave.remove();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    if (Math.random() > GRAVE_CHANCE.get()) return;

    MatchPlayer victim = event.getVictim();
    Player player = victim.getBukkit();

    // Remove previous grave if it exists
    Grave existingGrave = activeGraves.remove(victim.getId());
    if (existingGrave != null) {
      existingGrave.remove();
    }

    Grave newGrave = tryCreateGrave(player);
    if (newGrave == null) return;

    activeGraves.put(victim.getId(), newGrave);

    PGM.get()
        .getExecutor()
        .schedule(
            () -> {
              if (activeGraves.remove(victim.getId(), newGrave)) {
                newGrave.remove();
              }
            },
            GRAVE_ALIVE_SECONDS.get(),
            TimeUnit.SECONDS);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onEntityDeath(EntityDeathEvent event) {
    if (!event.getEntity().hasMetadata(GRAVE_META)) return;

    event.getDrops().clear();
    event.setDroppedExp(0);

    MetadataValue metadata = event.getEntity().getMetadata(GRAVE_META, Bingo.get());
    if (metadata == null) return;

    Grave targetGrave = activeGraves.get(UUID.fromString(metadata.asString()));
    if (targetGrave != null) {
      targetGrave.remove();
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onGravePunch(EntityDamageByEntityEvent event) {
    if (!event.getEntity().hasMetadata(GRAVE_META)) return;
    event.setCancelled(true);

    if (!(event.getDamager() instanceof Player)) return;

    MetadataValue metadata = event.getEntity().getMetadata(GRAVE_META, Bingo.get());
    if (metadata == null) return;

    Grave targetGrave = activeGraves.get(UUID.fromString(metadata.asString()));
    if (targetGrave == null) return;

    int punchCount = targetGrave.incrementAndGetPunchCount();

    ArmorStand head = targetGrave.getHead();
    EulerAngle currentPose = head.getHeadPose();
    head.setHeadPose(currentPose.add(Math.random() / 5, 0.2, Math.random() / 5));

    if (punchCount >= 3) {
      head.getWorld().dropItemNaturally(head.getLocation(), head.getEquipment().getHelmet());
      targetGrave.remove();
      activeGraves.remove(targetGrave.getOwner());
    }
  }

  private @Nullable Grave tryCreateGrave(Player player) {
    Location location = player.getLocation().clone();

    // Try to find solid block beneath
    Block base = location.getBlock();
    boolean foundGround = false;

    if (base.getType().isSolid()) {
      foundGround = true;
    } else {
      for (int i = 1; i <= DOWNWARDS_ALLOWANCE.get(); i++) {
        Block below = base.getRelative(BlockFace.DOWN, i);
        if (below.getType().isSolid()) {
          location.setY(below.getY() + 1);
          foundGround = true;
          break;
        }
      }
    }

    if (!foundGround) return null;

    return new Grave(player, location);
  }

  public static class Grave {

    private final UUID owner;
    private final List<ArmorStand> armorStands = new ArrayList<>();
    private final Slime slime;
    private final ArmorStand head;
    private int punchCount = 0;

    public Grave(Player victim, Location location) {
      this.owner = victim.getUniqueId();
      World world = location.getWorld();

      Vector direction = location.getDirection().setY(0).normalize();

      // Grave stone
      Location graveLocation = location.clone().subtract(0, 1.4, 0);
      ArmorStand grave = world.spawn(graveLocation, ArmorStand.class);
      grave.setMarker(true);
      grave.setVisible(false);
      grave.setBasePlate(false);
      NMSHacks.NMS_HACKS.freezeEntity(grave);
      grave.getEquipment().setHelmet(new ItemStack(Material.BRICK_STAIRS, 1));
      armorStands.add(grave);

      // Player head
      Location headLocation = location.clone().add(direction.multiply(0)).add(0, -0.42, 0);
      head = world.spawn(headLocation, ArmorStand.class);
      head.setMarker(true);
      head.setSmall(true);
      head.setVisible(false);
      head.setBasePlate(false);
      NMSHacks.NMS_HACKS.freezeEntity(head);
      head.setHelmet(getPlayerHead(victim));
      armorStands.add(head);

      // Slime hitbox
      slime = world.spawn(location.clone(), Slime.class);
      slime.setSize(1);

      slime.addPotionEffect(
          new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false),
          true);

      // No invulnerable/AI flags in 1.8, so freeze using NMS hacks if available
      NMSHacks.NMS_HACKS.freezeEntity(slime);
      slime.setMetadata(GRAVE_META, new FixedMetadataValue(Bingo.get(), owner));

      // Armor stand metadata
      grave.setMetadata(GRAVE_META, new FixedMetadataValue(Bingo.get(), owner));
      head.setMetadata(GRAVE_META, new FixedMetadataValue(Bingo.get(), owner));
    }

    public UUID getOwner() {
      return owner;
    }

    public ArmorStand getHead() {
      return head;
    }

    public int incrementAndGetPunchCount() {
      return ++punchCount;
    }

    public void remove() {
      armorStands.forEach(ArmorStand::remove);
      if (slime != null && !slime.isDead()) slime.remove();
    }

    private static ItemStack getPlayerHead(Player player) {
      ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
      SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
      skullMeta.setOwner(player.getName());
      skull.setItemMeta(skullMeta);
      return skull;
    }
  }
}
