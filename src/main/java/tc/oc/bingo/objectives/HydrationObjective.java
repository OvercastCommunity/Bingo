package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Tracker("stay-hydrated")
public class HydrationObjective extends ObjectiveTracker.Stateful<HydrationObjective.DrinkStatus> {

  private final Supplier<Integer> MAX_SECONDS = useConfig("max-seconds", 1800);
  private final Supplier<Integer> MIN_SECONDS = useConfig("min-seconds", 300);

  private final Supplier<Integer> REQUIRED_DRINKS = useConfig("required-drinks", 6);
  private final Supplier<Boolean> INCLUDE_POTIONS = useConfig("accept-potions", false);

  // when a player consumes a water bottle log the millisecond of when this was completed.
  // Check this against the stored last drink value,
  // if the number of seconds is between the min and max since the last time then increment
  // required-drinks by 1
  // If above the max reset drinks to 1
  // If below the min ignore the event

  // When the player reaches the required-drinks then reward them

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onDrink(PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    ItemStack item = event.getItem();
    if (item.getType() != Material.POTION) return;
    if (!INCLUDE_POTIONS.get() && item.getDurability() != 0) return;

    var data = getObjectiveData(player.getUniqueId());

    long now = System.currentTimeMillis();
    long timeSinceDrink = now - data.lastDrank;

    if (timeSinceDrink < MIN_SECONDS.get() * 1000L) return;

    int newCount = (timeSinceDrink > MAX_SECONDS.get() * 1000L ? 0 : data.drinkCount) + 1;
    storeObjectiveData(player.getUniqueId(), new DrinkStatus(newCount, now));

    if (newCount >= REQUIRED_DRINKS.get()) {
      reward(player);
    }
  }

  @Override
  public @NotNull HydrationObjective.DrinkStatus initial() {
    return new DrinkStatus(0, 0L);
  }

  @Override
  public @NotNull HydrationObjective.DrinkStatus deserialize(@NotNull String string) {
    String[] vals = string.split(",");
    // Backwards compat for pre-drink saving
    return new DrinkStatus(
        Integer.parseInt(vals[0]), vals.length > 1 ? Long.parseLong(vals[1]) : 0L);
  }

  @Override
  public @NotNull String serialize(@NotNull DrinkStatus data) {
    return data.drinkCount + "," + data.lastDrank;
  }

  @Override
  public double progress(DrinkStatus data) {
    return (double) data.drinkCount / REQUIRED_DRINKS.get();
  }

  public record DrinkStatus(int drinkCount, long lastDrank) {}
}
