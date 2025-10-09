package tc.oc.bingo.objectives;

import static tc.oc.bingo.modules.ItemRemoveCanceller.ITEM_META;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.bingo.modules.CustomPotionsModule;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("blindness-potion-task")
@DependsOn(CustomPotionsModule.class)
public class BlindnessPotionObjective extends ObjectiveTracker {

  @Override
  public void setupDependencies() {
    CustomPotionsModule.CustomPotionType customPotionType =
        new CustomPotionsModule.CustomPotionType(
            "blindness", this::checkIngredient, this::createPotion);

    CustomPotionsModule.INSTANCE.registerPotion("blindness", customPotionType);
  }

  @Override
  public void teardownDependencies() {
    CustomPotionsModule.INSTANCE.removePotion("blindness");
  }

  private Boolean checkIngredient(ItemStack itemStack) {
    // Require blaze powder to brew
    if (!itemStack.getType().equals(Material.GHAST_TEAR)) return false;

    // Require item to be a custom item
    return ITEM_META.has(itemStack);
  }

  private ItemStack createPotion() {
    return CustomPotionsModule.createPotion(
        "§8Potion of Sightlessness",
        List.of("§cBlindness (00:30)", "§7I senses something wrong..."),
        (short) 14);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerConsumePotion(CustomPotionsModule.CustomPotionDrinkEvent event) {
    Player player = event.getPlayer();

    if (!event.getSlug().equals("blindness")) return;
    if (!CustomPotionsModule.hasEffect(player, "blindness")) return;

    event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 600, 1));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer matchPlayer = getStatePlayer(event.getKiller());
    if (matchPlayer == null) return;

    Player player = matchPlayer.getBukkit();

    if (!CustomPotionsModule.hasEffect(player, "blindness")) return;

    reward(player);
  }
}
