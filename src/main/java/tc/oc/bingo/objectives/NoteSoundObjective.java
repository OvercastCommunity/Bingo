package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@Tracker("note-sounds")
public class NoteSoundObjective extends ObjectiveTracker {

  private final Map<UUID, Location> playLocation = useState(Scope.LIFE);
  private final Map<UUID, EnumSet<Instrument>> notesPlayed = useState(Scope.MATCH);

  private final Supplier<Integer> REQUIRED_TYPES = useConfig("required-types", 5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK
        || event.getAction() == Action.LEFT_CLICK_BLOCK)) return;

    Block block = event.getClickedBlock();
    if (block == null || block.getType() != Material.NOTE_BLOCK) return;

    Player player = event.getPlayer();
    if (player == null) return;

    playLocation.put(player.getUniqueId(), block.getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onNotePlay(NotePlayEvent event) {
    Block block = event.getBlock();
    if (block.getType() != Material.NOTE_BLOCK) return;

    Player player =
        block.getLocation().getNearbyPlayers(3).stream()
            .filter(
                p -> {
                  Location location = playLocation.get(p.getUniqueId());
                  playLocation.remove(p.getUniqueId());
                  return location.equals(block.getLocation());
                })
            .findFirst()
            .orElse(null);

    if (player == null) return;

    Set<Instrument> playerNotes =
        notesPlayed.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Instrument.class));

    playerNotes.add(event.getInstrument());

    if (playerNotes.size() >= REQUIRED_TYPES.get()) {
      reward(player);
    }
  }
}
