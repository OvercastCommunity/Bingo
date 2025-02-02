package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("item-bearer")
public class FlowerBearerObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> REQUIRED_INTERACTIONS = useConfig("required-interactions", 12);
  private final Supplier<Integer> EFFECT_COOLDOWN = useConfig("cooldown-seconds", 5);

  private final Supplier<Boolean> PLAY_EFFECT = useConfig("play-effect", true);

  private final Supplier<Set<Material>> REQUIRED_ITEMS =
      useConfig(
          "block-list", Set.of(Material.RED_ROSE, Material.YELLOW_FLOWER), MATERIAL_SET_READER);

  private final Map<UUID, Set<UUID>> interactedPlayers = useState(Scope.LIFE);

  private final Map<UUID, Long> playerCooldown = useState(Scope.LIFE);

  private Match match;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    this.match = event.getMatch();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof Player target)) return;

    Player player = event.getPlayer();
    ItemStack itemInHand = player.getInventory().getItemInHand();

    if (!REQUIRED_ITEMS.get().contains(itemInHand.getType())) return;

    // Ensure unique interaction in this life
    UUID playerId = player.getUniqueId();
    UUID targetId = target.getUniqueId();

    // Check that the cooldown is applied
    Long cooldown = playerCooldown.get(targetId);
    if (cooldown != null && System.currentTimeMillis() - cooldown < (EFFECT_COOLDOWN.get() * 1000))
      return;
    playerCooldown.put(targetId, System.currentTimeMillis());

    MatchPlayer matchPlayer = getPlayer(player);
    MatchPlayer targetPlayer = getPlayer(target);

    if (matchPlayer == null || targetPlayer == null) return;

    itemInHand.setAmount(itemInHand.getAmount() - 1);
    player.getInventory().setItemInHand(itemInHand);

    if (PLAY_EFFECT.get()) {
      new HeartEffectTask(match, target);
      Bukkit.getPluginManager().callEvent(new PlayerWooHooEvent(matchPlayer, targetPlayer));
    }

    if (!interactedPlayers.computeIfAbsent(playerId, uuid -> new HashSet<>()).add(targetId)) return;
    Integer interactions = updateObjectiveData(playerId, count -> count + 1);

    // Check if the player has completed the objective
    if (interactions >= REQUIRED_INTERACTIONS.get()) {
      reward(player);
    }
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return (double) data / REQUIRED_INTERACTIONS.get();
  }

  @Getter
  public static class PlayerWooHooEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final MatchPlayer player;
    private final MatchPlayer target;

    public PlayerWooHooEvent(MatchPlayer player, MatchPlayer target) {
      this.player = player;
      this.target = target;
    }
  }

  protected static class HeartEffectTask implements Runnable {
    private final Match match;
    private final Player player;
    private final ScheduledFuture<?> scheduledFuture;
    private int remaining = 5;
    private int particles = 5;

    private HeartEffectTask(Match match, Player player) {
      this.match = match;
      this.player = player;
      scheduledFuture =
          match.getExecutor(MatchScope.LOADED).scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
      if (--remaining <= 0 || !match.isRunning()) scheduledFuture.cancel(false);
      playEffect();
    }

    public void playEffect() {
      for (int i = 0; i < Math.max(3, particles); i++) {
        Location spawnLocation =
            player
                .getLocation()
                .add(
                    (Math.random() - 0.5) * 1.5,
                    1.8 + Math.random() * 1.2,
                    (Math.random() - 0.5) * 1.5);
        player.getWorld().spigot().playEffect(spawnLocation, Effect.HEART, 0, 0, 0, 0, 0, 1, 5, 64);
      }
      particles--;
    }
  }
}
