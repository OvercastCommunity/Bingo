package tc.oc.bingo.objectives;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

import java.util.function.Supplier;

@Tracker("kill-jump-scare")
public class KillJumpScareObjective extends ObjectiveTracker {

  // A random chance to strike lightning, put a pumpkin on the player's head, and to play a sound on kill
  // Only is rolled if the player does not currently have a helmet equipped

  private final Supplier<Double> TRIGGER_CHANCE = useConfig("trigger-chance", 0.2);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;
    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null || killer.getInventory() == null || killer.getInventory().getHelmet() != null) return;

    if (Math.random() >= TRIGGER_CHANCE.get()) return;

    // Only trigger once per player
    if (hasCompleted(killer.getBukkit())) return;

    killer.getInventory().setHelmet(new ItemStack(Material.PUMPKIN));
    killer.playSound(Sound.sound(Key.key("mob.guardian.curse"), Sound.Source.MASTER, 1f, 1f));
    killer.getWorld().strikeLightningEffect(killer.getLocation());
  }
}