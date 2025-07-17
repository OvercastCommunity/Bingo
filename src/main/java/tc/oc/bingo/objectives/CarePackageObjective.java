package tc.oc.bingo.objectives;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import tc.oc.bingo.modules.CarePackageModule;
import tc.oc.bingo.modules.DependsOn;

@Tracker("care-package")
@DependsOn(CarePackageModule.class)
public class CarePackageObjective extends ObjectiveTracker {

  @EventHandler
  public void onCarePackageOpen(CarePackageModule.CarePackageOpenEvent event) {
    Player player = event.getPlayer();
    if (event.getCarePackage().getOwner().equals(player.getUniqueId())) {
      reward(player);
    }
  }
}
