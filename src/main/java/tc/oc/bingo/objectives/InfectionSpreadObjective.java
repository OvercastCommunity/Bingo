package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("infection-spread")
public class InfectionSpreadObjective extends ObjectiveTracker {

  private final Supplier<Double> EAT_INFECTION_CHANCE = useConfig("eat-infection-chance", 0.2);
  private final Supplier<String> INFECTION_TIME = useConfig("infection-time", "3d");

  private final Supplier<Double> MIN_CHANCE = useConfig("min-chance", 0.005);
  private final Supplier<Double> MAX_CHANCE = useConfig("max-chance", 0.03);
  private final Supplier<Double> CHANCE_SPREAD =
      useComputedConfig(() -> MAX_CHANCE.get() - MIN_CHANCE.get());

  private final Supplier<Double> MIN_DISTANCE = useConfig("min-distance", 2d);
  private final Supplier<Double> MAX_DISTANCE = useConfig("max-distance", 7d);
  private final Supplier<Double> DISTANCE_SPREAD =
      useComputedConfig(() -> MAX_DISTANCE.get() - MIN_DISTANCE.get());
  private final Supplier<Double> SLOPE =
      useComputedConfig(() -> CHANCE_SPREAD.get() / DISTANCE_SPREAD.get());

  public static final String INFECTED_GROUP = "bingo.infected";
  public static final String INFECTED_PERMISSION = "group." + INFECTED_GROUP;
  private static final String PERMISSION_COMMAND = "lp user %s parent addtemp %s %s";

  private final Random random = new Random();

  private final Set<Material> RAW_FOODS =
      EnumSet.of(Material.RAW_FISH, Material.RAW_BEEF, Material.RAW_CHICKEN, Material.ROTTEN_FLESH);

  private boolean allowSelfInfection = false;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    this.allowSelfInfection = false;

    // 30 seconds after match starts check if there are any infected players in the game
    PGM.get()
        .getExecutor()
        .schedule(
            () -> {
              // If there are no infected players allow self-infection
              if (!event.getMatch().isRunning()) return;

              allowSelfInfection =
                  event.getMatch().getParticipants().stream()
                      .noneMatch(p -> p.getBukkit().hasPermission(INFECTED_PERMISSION));
            },
            30,
            TimeUnit.SECONDS);
  }

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    Player victim = getBukkit(event.getVictim());
    Player killer = getBukkit(event.getKiller());
    if (victim == null || killer == null) return;

    boolean killerInfected = killer.hasPermission(INFECTED_PERMISSION);
    boolean victimInfected = victim.hasPermission(INFECTED_PERMISSION);

    // If both infected or not-infected do nothing
    if (killerInfected == victimInfected) return;

    double dist =
        Math.max(victim.getLocation().distance(killer.getLocation()) - MIN_DISTANCE.get(), 0);
    if (dist >= DISTANCE_SPREAD.get()) return;
    double infectionChance = MAX_CHANCE.get() - (dist * SLOPE.get());

    if (random.nextDouble() >= infectionChance) return;

    if (!killerInfected) grantInfection(killer);
    if (!victimInfected) grantInfection(victim);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemConsume(PlayerItemConsumeEvent event) {
    if (!allowSelfInfection) return;

    Material type = event.getItem().getType();
    if (RAW_FOODS.contains(type) && random.nextDouble() < EAT_INFECTION_CHANCE.get()) {
      grantInfection(event.getPlayer());
      allowSelfInfection = false;
    }
  }

  private void grantInfection(Player player) {
    String cmd =
        PERMISSION_COMMAND.formatted(player.getUniqueId(), INFECTED_GROUP, INFECTION_TIME.get());
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null) return;

    reward(player);
    matchPlayer.sendMessage(text("☠ You have been infected! ☠", NamedTextColor.GREEN));

    matchPlayer.getMatch().callEvent(new NameDecorationChangeEvent(matchPlayer.getId()));
  }
}
