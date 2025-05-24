package tc.oc.bingo.objectives;

import java.util.stream.Stream;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import tc.oc.bingo.modules.CarePackageModule;
import tc.oc.bingo.util.ManagedListener;

@Tracker("care-package")
public class CarePackageObjective extends ObjectiveTracker {

  @Override
  public Stream<ManagedListener> children() {
    return Stream.concat(super.children(), Stream.of(new CarePackageModule()));
  }

  @EventHandler
  public void onCarePackageOpen(CarePackageModule.CarePackageOpenEvent event) {
    Player player = event.getPlayer();
    if (event.getCarePackage().getOwner().equals(player.getUniqueId())) {
      reward(player);
    }
  }
}
