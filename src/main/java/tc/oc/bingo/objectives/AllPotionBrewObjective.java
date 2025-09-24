package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import tc.oc.bingo.modules.CustomPotionsModule;
import tc.oc.bingo.modules.DependsOn;

@Tracker("all-potion-brew")
@DependsOn(CustomPotionsModule.class)
public class AllPotionBrewObjective extends ObjectiveTracker.StatefulSet<String> {

  private final Supplier<Integer> REQUIRED_AMOUNT = useConfig("required-amount", 5);

  @EventHandler
  public void onCustomPotionBrew(CustomPotionsModule.CustomPotionBrewEvent event) {
    String customPotionType = CustomPotionsModule.isCustomPotion(event.getItemStack());
    if (customPotionType == null) return;

    trackProgress(event.getPlayer(), customPotionType);
  }

  @Override
  protected int maxCount() {
    return REQUIRED_AMOUNT.get();
  }
}
