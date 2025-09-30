package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomPotionsModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("streaker-potion-task")
public class StreakerPotionObjective extends ObjectiveTracker {

  private final Map<UUID, Double> playerMaxHealths = useState(Scope.LIFE);
  private final Map<UUID, Integer> playerStreak = useState(Scope.LIFE);

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 5);

  private final Supplier<Double> MAX_EXTRA_HEARTS = useConfig("max-extra-hearts", 10d);
  private final Supplier<Double> HEARTS_PER_KILL = useConfig("hearts-per-kill", 1d);

  private final Supplier<Boolean> RESET_ON_CONSUME = useConfig("reset-on-consume", false);

  private final Set<Material> SWORD_MATERIALS =
      EnumSet.of(
          Material.WOOD_SWORD,
          Material.STONE_SWORD,
          Material.GOLD_SWORD,
          Material.IRON_SWORD,
          Material.DIAMOND_SWORD);

  @Override
  public void setupDependencies() {
    CustomPotionsModule.CustomPotionType customPotionType =
        new CustomPotionsModule.CustomPotionType(
            "streaker", this::checkIngredient, this::createPotion);

    CustomPotionsModule.INSTANCE.registerPotion("streaker", customPotionType);
  }

  @Override
  public void teardownDependencies() {
    CustomPotionsModule.INSTANCE.removePotion("streaker");
  }

  private Boolean checkIngredient(ItemStack itemStack) {
    return SWORD_MATERIALS.contains(itemStack.getType());
  }

  private ItemStack createPotion() {
    return CustomPotionsModule.createPotion(
        "§ePotion of Streaking",
        List.of("§9Streaking (05:00)", "§7Ultimate absorption"),
        (short) 6,
        300);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer matchPlayer = getStatePlayer(event.getKiller());
    if (matchPlayer == null) return;

    if (!CustomPotionsModule.hasEffect(matchPlayer.getBukkit(), "streaker")) return;

    // Increment and get the new streak
    int newStreak = playerStreak.merge(matchPlayer.getId(), 1, Integer::sum);

    updatePlayerHealth(matchPlayer.getBukkit(), newStreak);

    if (newStreak >= KILLS_REQUIRED.get()) {
      reward(matchPlayer.getBukkit());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPotionConsume(CustomPotionsModule.CustomPotionDrinkEvent event) {
    if (!event.getSlug().equals("streaker")) return;

    if (RESET_ON_CONSUME.get()) {
      playerStreak.remove(event.getPlayer().getUniqueId());
      resetPlayerHealth(event.getPlayer());
    }

    // Do not track if already tracked (would allow for stacking)
    UUID playerId = event.getPlayer().getUniqueId();
    if (playerMaxHealths.containsKey(playerId)) return;

    playerMaxHealths.put(playerId, event.getPlayer().getMaxHealth());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPotionExpire(CustomPotionsModule.CustomPotionExpireEvent event) {
    if (!event.getSlug().equals("streaker")) return;

    // Reset the potion-ed streak
    playerStreak.remove(event.getPlayer().getUniqueId());

    // Set the player back to their default health
    resetPlayerHealth(event.getPlayer());
  }

  private void updatePlayerHealth(Player player, int streak) {
    Double defaultMaxHealth = playerMaxHealths.getOrDefault(player.getUniqueId(), 20.0);

    double newHealth =
        Math.min(
            defaultMaxHealth + (streak * HEARTS_PER_KILL.get()),
            defaultMaxHealth + (MAX_EXTRA_HEARTS.get()));
    player.setMaxHealth(newHealth);
  }

  private void resetPlayerHealth(Player player) {
    Double previousHealthValue = playerMaxHealths.getOrDefault(player.getUniqueId(), 20.0);
    player.setMaxHealth(previousHealthValue);
  }

  @Override
  public Double getProgress(UUID uuid) {
    return computeProgress(playerStreak.get(uuid), KILLS_REQUIRED.get());
  }
}
