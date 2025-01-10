package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("item-bearer")
public class FlowerBearerObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> REQUIRED_INTERACTIONS = useConfig("required-interactions", 12);

  private final Supplier<Boolean> PLAY_EFFECT = useConfig("play-effect", true);

  private final Supplier<Set<Material>> REQUIRED_ITEMS =
      useConfig(
          "block-list", Set.of(Material.RED_ROSE, Material.YELLOW_FLOWER), MATERIAL_SET_READER);

  private final Map<UUID, Set<UUID>> interactedPlayers = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof Player target)) return;

    Player player = event.getPlayer();
    ItemStack itemInHand = player.getInventory().getItemInHand();

    if (!REQUIRED_ITEMS.get().contains(itemInHand.getType())) return;

    // Ensure unique interaction in this life
    UUID playerId = player.getUniqueId();
    UUID targetId = target.getUniqueId();

    if (!interactedPlayers.computeIfAbsent(playerId, uuid -> new HashSet<>()).add(targetId)) return;

    MatchPlayer matchPlayer = getPlayer(player);
    MatchPlayer targetPlayer = getPlayer(target);

    if (matchPlayer == null || targetPlayer == null) return;

    Integer interactions = updateObjectiveData(playerId, count -> count + 1);
    itemInHand.setAmount(itemInHand.getAmount() - 1);

    if (PLAY_EFFECT.get()) {
      target.getWorld().playEffect(target.getLocation().add(0, 1, 0), Effect.HEART, 10);
      Bukkit.getPluginManager().callEvent(new PlayerWooHooEvent(matchPlayer, targetPlayer));
    }

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
}
