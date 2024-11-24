package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("shared-item")
public class SharedItemObjective extends ObjectiveTracker {

  private final Map<Integer, UUID> itemThrowers = new HashMap<>();

  private final Supplier<Set<Material>> TRACKED_MATERIAL =
      useConfig("material", Set.of(Material.GOLDEN_APPLE), MATERIAL_SET_READER);

  private final Supplier<Integer> TRACKED_DURATION = useConfig("pickup-seconds", 5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    itemThrowers.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if (!TRACKED_MATERIAL.get().contains(event.getItemDrop().getItemStack().getType())) return;

    MatchPlayer player = getPlayer(event.getPlayer());
    if (player == null) return;

    int entityId = event.getItemDrop().getEntityId();
    itemThrowers.put(entityId, player.getId());

    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Bingo.get(), () -> itemThrowers.remove(entityId), TRACKED_DURATION.get() * 20L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickUp(PlayerPickupItemEvent event) {
    UUID thrower = itemThrowers.getOrDefault(event.getItem().getEntityId(), null);
    if (thrower == null) return;
    itemThrowers.remove(event.getItem().getEntityId());

    MatchPlayer picker = getPlayer(event.getPlayer());
    if (picker == null || picker.getId().equals(thrower)) return;

    MatchPlayer matchPlayer = getPlayer(thrower);
    if (matchPlayer == null) return;

    reward(matchPlayer.getBukkit());
  }
}
