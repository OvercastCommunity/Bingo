package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.regions.CylindricalRegion;

@Tracker("fishing-festival")
public class FishingFestivalObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 10);
  private final Supplier<Integer> MAX_TIME_WINDOW = useConfig("max-time-window", 30);

  private final Supplier<Integer> FESTIVAL_RANGE = useConfig("festival-range", 5);
  private final Supplier<Integer> FESTIVAL_TIME = useConfig("festival-seconds", 60);
  private final Supplier<Integer> FESTIVAL_COOLDOWN = useConfig("festival-cooldown-seconds", 300);

  private final Supplier<Integer> MIN_DUST = useConfig("min-dust", 3);
  private final Supplier<Integer> MAX_DUST = useConfig("max-dust", 8);

  private final Map<UUID, Vector> playerCatchLocations = useState(Scope.LIFE);
  private final Map<UUID, Long> playerCatchTimestamps = useState(Scope.LIFE);

  private CylindricalRegion festivalRegion = null;
  private Long festivalEndTime = null;
  private Future<?> countdownTask = null;

  private static final Random RANDOM = new Random();
  private Match match;

  @Override
  public Stream<ManagedListener> children() {
    return Stream.concat(
        super.children(),
        Stream.of(
            new ManagedListener.Ticker(
                this::spawnLocationParticles, 0, 250, TimeUnit.MILLISECONDS)));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    tryResetFestival();
  }

  private void tryResetFestival() {
    if (countdownTask == null) return;

    countdownTask.cancel(false);
    Bukkit.broadcastMessage(ChatColor.GOLD + "The fishing festival has concluded!");
    festivalEndTime = System.currentTimeMillis();
    festivalRegion = null;
    match = null;
    countdownTask = null;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
    if (!(event.getCaught() instanceof Item item)) return;

    Player player = event.getPlayer();
    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null) return;

    UUID playerId = player.getUniqueId();
    Vector location = event.getHook().getLocation().toVector();
    long currentTime = System.currentTimeMillis();

    // Store the latest fishing catch for this player
    playerCatchLocations.put(playerId, location);
    playerCatchTimestamps.put(playerId, currentTime);

    // Check if within fishing festival region
    if (festivalRegion != null && festivalRegion.contains(location)) {
      item.setItemStack(
          new ItemStack(
              Material.GLOWSTONE_DUST, RANDOM.nextInt(MIN_DUST.get(), MAX_DUST.get() + 1)));

      reward(player);
      return;
    }

    // Festival already running, no need to attempt another
    if (countdownTask != null) return;

    // Festival is on a cooldown
    if (festivalEndTime != null
        && currentTime - festivalEndTime <= FESTIVAL_COOLDOWN.get() * 1000) {
      return;
    }

    // TODO: Require a catch to be in non-flowing water
    // event.getHook().getLocation().getBlock();

    // Check for a match with another player
    for (UUID otherId : playerCatchTimestamps.keySet()) {
      if (otherId.equals(playerId)) continue;
      Vector otherLoc = playerCatchLocations.get(otherId);
      Long otherTime = playerCatchTimestamps.get(otherId);

      if (otherLoc != null
          && otherTime != null
          && location.distance(otherLoc) <= MAX_DISTANCE.get()
          && currentTime - otherTime <= MAX_TIME_WINDOW.get() * 1000) {

        // When two players meet this criteria:
        // Set the festival location as the midpoint between their catches
        match = matchPlayer.getMatch();
        festivalRegion =
            new CylindricalRegion(
                otherLoc.getMidpoint(location).subtract(new Vector(0, 1.5, 0)),
                FESTIVAL_RANGE.get(),
                3);

        // Broadcast a message announcing the start of a fishing festival
        match.sendMessage(
            text("A fishing festival has started! Catch fish near ", NamedTextColor.GOLD)
                .append(matchPlayer.getName())
                .append(text("!")));

        // Start a countdown that resets the festival location after FESTIVAL_TIME seconds
        countdownTask =
            PGM.get()
                .getExecutor()
                .schedule(this::tryResetFestival, FESTIVAL_TIME.get(), TimeUnit.SECONDS);

        // Do not reward yet.
        return;
      }
    }
  }

  private void spawnLocationParticles() {
    if (festivalRegion == null) return;

    // Randomly select a point that is within range of the festival location
    double centerY = festivalRegion.getBounds().getCenterPoint().getY();
    for (int i = 0; i < 10; i++) {
      Location location = festivalRegion.getRandom(RANDOM).toLocation(match.getWorld());
      location.setY(centerY);

      match.getWorld().playEffect(location, Effect.WATERDRIP, 1);
    }
  }
}
