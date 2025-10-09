package tc.oc.bingo.modules;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntitySlime;
import net.minecraft.server.v1_8_R3.EntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.objectives.Scope;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.reflect.ReflectionUtils;

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

  @EventHandler(priority = EventPriority.MONITOR)
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

    Grave targetGrave = activeGraves.remove(UUID.fromString(metadata.asString()));
    if (targetGrave != null) {
      targetGrave.remove();
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onGravePunch(EntityDamageByEntityEvent event) {
    if (!event.getEntity().hasMetadata(GRAVE_META)) return;
    event.setCancelled(true);

    if (!(event.getDamager() instanceof Player player)) return;

    MetadataValue metadata = event.getEntity().getMetadata(GRAVE_META, Bingo.get());
    if (metadata == null) return;

    Grave targetGrave = activeGraves.get(UUID.fromString(metadata.asString()));
    if (targetGrave == null) return;

    int punchCount = targetGrave.incrementAndGetPunchCount();

    ArmorStand head = targetGrave.getHead();
    EulerAngle currentPose = head.getHeadPose();
    head.setHeadPose(currentPose.add(Math.random() / 5, 0.2, Math.random() / 5));
    event.getWorld().playSound(targetGrave.getHead().getLocation(), Sound.DIG_STONE, 1.0f, 1.0f);

    if (punchCount >= 3) {
      head.getWorld()
          .dropItemNaturally(head.getLocation().add(0, 0.5, 0), head.getEquipment().getHelmet());
      targetGrave.remove();
      activeGraves.remove(targetGrave.getOwner(), targetGrave);

      GraveBreakEvent graveBreakEvent = new GraveBreakEvent(player, targetGrave);
      Bukkit.getPluginManager().callEvent(graveBreakEvent);
    }
  }

  private @Nullable Grave tryCreateGrave(Player player) {
    Location location = player.getLocation();

    // Try to find solid block beneath
    Block curr = location.getBlock();
    for (int i = 0; i <= DOWNWARDS_ALLOWANCE.get(); i++) {
      if (Materials.isSolid(curr.getType())) return new Grave(player, location);
      curr = curr.getRelative(BlockFace.DOWN);
      location.setY(curr.getY() + 1);
    }
    return null;
  }

  public static class Grave {
    @Getter private final UUID owner;
    @Getter private final ArmorStand head;
    private final List<Entity> entities = new ArrayList<>(3);
    private int punchCount = 0;

    public Grave(Player victim, Location location) {
      this.owner = victim.getUniqueId();
      location.setPitch(0);

      World world = location.getWorld();

      // Grave stone
      ArmorStand grave = world.spawn(location.clone().subtract(0, 1.4, 0), ArmorStand.class);
      grave.setMarker(true);
      grave.setVisible(false);
      grave.setBasePlate(false);
      grave.getEquipment().setHelmet(new ItemStack(Material.BRICK_STAIRS, 1));
      NMS_HACKS.freezeEntity(grave);
      entities.add(grave);

      // Player head
      head = world.spawn(location.clone().add(0, -0.42, 0), ArmorStand.class);
      head.setMarker(true);
      head.setSmall(true);
      head.setVisible(false);
      head.setBasePlate(false);
      head.setHelmet(getPlayerHead(victim));
      NMS_HACKS.freezeEntity(head);
      entities.add(head);

      // Interaction hitbox
      var interaction = new InteractionEntity(location.clone().add(0, 0.05, 0));
      entities.add(interaction.getBukkitEntity());

      // Set metadata
      for (Entity entity : entities)
        entity.setMetadata(GRAVE_META, new FixedMetadataValue(Bingo.get(), owner));
    }

    public int incrementAndGetPunchCount() {
      return ++punchCount;
    }

    public void remove() {
      entities.forEach(Entity::remove);
    }

    private static ItemStack getPlayerHead(Player player) {
      ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
      SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
      skullMeta.setOwner(player.getName());
      skull.setItemMeta(skullMeta);
      return skull;
    }
  }

  @Getter
  public static class GraveBreakEvent extends Event {
    private final Player player;
    private final Grave grave;

    private static final HandlerList handlers = new HandlerList();

    public GraveBreakEvent(Player player, Grave grave) {
      this.player = player;
      this.grave = grave;
    }
  }

  public static class InteractionEntity extends EntitySlime {
    static {
      // Register FakeSlime as the same as EntitySlime in entity types
      var a = ReflectionUtils.readStaticField(EntityTypes.class, Map.class, "d");
      var b = ReflectionUtils.readStaticField(EntityTypes.class, Map.class, "f");
      a.put(InteractionEntity.class, a.get(EntitySlime.class));
      b.put(InteractionEntity.class, b.get(EntitySlime.class));
    }

    public InteractionEntity(Location loc) {
      super(((CraftWorld) loc.getWorld()).getHandle());
      setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
      setSize(1);
      setInvisible(true);
      k(true); // NoAI = true

      world.addEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    @Override
    public boolean a(EntityPlayer entityplayer) {
      // Only show the entity to players that cannot see invisible entities (ie: joined players)
      return !entityplayer.getBukkitEntity().canSeeInvisibles();
    }

    @Override
    protected void B() {
      // No ticking effects: usually toggles invisibility
    }
  }
}
