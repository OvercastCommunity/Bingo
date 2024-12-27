package tc.oc.bingo.objectives;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("band-practice")
public class BandPracticeObjective extends ObjectiveTracker {

  private final Map<UUID, PlayEntry> players = useState(Scope.LIFE);

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 8);
  private final Supplier<Integer> MAX_SECONDS = useConfig("max-seconds", 5);
  private final Supplier<Integer> MIN_INSTRUMENTS = useConfig("min-instruments", 4);

  // Track the last playing person and location
  private UUID lastPlayedPlayer = null;
  private Location lastPlayedAt = null;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK
        || event.getAction() == Action.LEFT_CLICK_BLOCK)) return;

    Block block = event.getClickedBlock();
    if (block == null || block.getType() != Material.NOTE_BLOCK) return;

    Player player = event.getPlayer();
    if (player == null) return;

    lastPlayedPlayer = player.getUniqueId();
    lastPlayedAt = block.getLocation();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onNotePlay(NotePlayEvent event) {
    Block block = event.getBlock();
    if (block.getType() != Material.NOTE_BLOCK) return;

    UUID player = lastPlayedPlayer;
    Location location = lastPlayedAt;

    lastPlayedPlayer = null;
    lastPlayedAt = null;

    if (location == null || !location.equals(block.getLocation())) return;

    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null) return;

    playerPlayedNoteBlock(matchPlayer, event);
  }

  private void playerPlayedNoteBlock(MatchPlayer player, NotePlayEvent event) {
    Instant now = Instant.now();
    Instant limit = now.minusSeconds(MAX_SECONDS.get());

    players.put(
        player.getId(), new PlayEntry(event.getBlock().getLocation(), event.getInstrument(), now));

    EnumSet<Instrument> instruments = EnumSet.noneOf(Instrument.class);
    List<Player> validPlayers =
        player.getBukkit().getLocation().getNearbyPlayers(MAX_DISTANCE.get()).stream()
            .map(
                p -> {
                  UUID playerId = p.getUniqueId();
                  PlayEntry playEntry = players.get(playerId);
                  if (playEntry == null) return null;

                  // Record is no longer valid so remove globally
                  if (playEntry.time.isBefore(limit)) {
                    players.remove(playerId);
                    return null;
                  }

                  instruments.add(playEntry.instrument);
                  return p;
                })
            .filter(Objects::nonNull)
            .toList();

    if (instruments.size() >= MIN_INSTRUMENTS.get()) {
      reward(validPlayers);
    }
  }

  record PlayEntry(Location location, Instrument instrument, Instant time) {}
}
