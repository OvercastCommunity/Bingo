package tc.oc.bingo.objectives;

import static net.kyori.adventure.text.Component.text;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("infection-spread")
public class InfectionSpreadObjective extends ObjectiveTracker implements Listener {

  private final Supplier<Double> BASE_INFECTION_CHANCE =
      useConfig("infection-chance", 0.001); // Higher base chance
  private final Supplier<Double> MIN_DISTANCE_CHANCE = useConfig("min-distance-chance", 0.02);
  private final Supplier<Double> MAX_DISTANCE_CHANCE = useConfig("max-distance-chance", 0.001);

  private final Supplier<Double> MAX_DISTANCE = useConfig("max-distance", 5d);
  private final Supplier<String> PERMISSION_COMMAND =
      useConfig("permission-command", "lp user %s permission set %s");

  public final String INFECTED_GROUP = "group.bingo.infected";

  private final Random random = new Random();

  private final Set<Material> RAW_FOODS =
      EnumSet.of(Material.RAW_FISH, Material.RAW_BEEF, Material.RAW_CHICKEN, Material.ROTTEN_FLESH);

  private boolean allowSelfInfection = false;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    this.allowSelfInfection = false;

    // 30 seconds after match starts check if there are any infected players in the game
    Bukkit.getScheduler()
        .runTaskLater(
            Bingo.get(),
            () -> {
              // If there are no infected players allow self-infection
              if (!event.getMatch().isRunning()) return;

              boolean containsInfected =
                  event.getMatch().getParticipants().stream()
                      .anyMatch(p -> p.getBukkit().hasPermission(INFECTED_GROUP));

              allowSelfInfection = !containsInfected;
            },
            600L);
  }

  @EventHandler
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    Player victim = event.getPlayer().getBukkit();
    if (victim == null) return;

    if (!(getPlayer(event.getKiller()) instanceof Player killer)) return;

    boolean killerInfected = killer.hasPermission(INFECTED_GROUP);
    boolean victimInfected = victim.hasPermission(INFECTED_GROUP);

    // If both infected or not-infected do nothing
    if ((killerInfected && victimInfected) || (!killerInfected && !victimInfected)) return;

    double distance = victim.getLocation().distance(killer.getLocation());
    if (distance >= MAX_DISTANCE.get()) return;

    double infectionChance =
        MIN_DISTANCE_CHANCE.get()
            - ((distance / MAX_DISTANCE.get())
                * (MIN_DISTANCE_CHANCE.get() - MAX_DISTANCE_CHANCE.get()));

    if (!(random.nextDouble() < infectionChance)) return;

    if (!killerInfected) {
      grantInfection(killer);
    }

    if (!victimInfected) {
      grantInfection(victim);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemConsume(PlayerItemConsumeEvent event) {
    if (!allowSelfInfection) return;

    Material type = event.getItem().getType();
    if (RAW_FOODS.contains(type)) {
      grantInfection(event.getPlayer());
      allowSelfInfection = false;
    }
  }

  private void grantInfection(Player player) {
    String command = String.format(PERMISSION_COMMAND.get(), player.getName(), INFECTED_GROUP);

    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null) return;

    reward(player);

    matchPlayer.sendMessage(text("☠ You have been infected! ☠", NamedTextColor.GREEN));
  }
}
