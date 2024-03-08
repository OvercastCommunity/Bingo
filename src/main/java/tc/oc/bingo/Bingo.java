package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.bingo.objectives.ArmourSharedObjective;
import tc.oc.bingo.objectives.CleanMatchObjective;
import tc.oc.bingo.objectives.CobwebKillerObjective;
import tc.oc.bingo.objectives.DefenderKillObjective;
import tc.oc.bingo.objectives.EatingObjective;
import tc.oc.bingo.objectives.EnchantItemObjective;
import tc.oc.bingo.objectives.FisherthemObjective;
import tc.oc.bingo.objectives.FlowerPotObjective;
import tc.oc.bingo.objectives.HedgehogObjective;
import tc.oc.bingo.objectives.KillStreakObjective;
import tc.oc.bingo.objectives.MatchLengthObjective;
import tc.oc.bingo.objectives.Objective;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.PlayerCraftObjective;
import tc.oc.bingo.objectives.PlayerShiftingObjective;
import tc.oc.bingo.objectives.PotionConsumeObjective;
import tc.oc.bingo.objectives.QuickKillsObjective;
import tc.oc.bingo.config.Config;

public class Bingo extends JavaPlugin {

  private static Bingo plugin;
  private Manager manager;

  private BukkitCommandManager commands;

  @Override
  public void onEnable() {
    plugin = this;

    saveDefaultConfig();
    Config.create(getConfig());

    getObjectives()
        .forEach(
            objectiveTracker -> {
              getServer().getPluginManager().registerEvents(objectiveTracker, this);
            });
  }

  public Collection<ObjectiveTracker> getObjectives() {
    return Collections.unmodifiableList(
        Arrays.asList(
            new CleanMatchObjective(new Objective("CleanMatch", "CleanMatch", "CleanMatch")),
            new CobwebKillerObjective(
                new Objective("CobwebKiller", "CobwebKiller", "CobwebKiller")),
            new DefenderKillObjective(
                new Objective("DefenderKill", "DefenderKill", "DefenderKill")),
            new EatingObjective(new Objective("Eating", "Eating", "Eating")),
            new EnchantItemObjective(new Objective("EnchantItem", "EnchantItem", "EnchantItem")),
            new FisherthemObjective(new Objective("Fisherthem", "Fisherthem", "Fisherthem")),
            new FlowerPotObjective(new Objective("FlowerPot", "FlowerPot", "FlowerPot")),
            new KillStreakObjective(new Objective("KillStreak", "KillStreak", "KillStreak")),
            new MatchLengthObjective(new Objective("MatchLength", "MatchLength", "MatchLength")),
            new PlayerCraftObjective(new Objective("PlayerCraft", "PlayerCraft", "PlayerCraft")),
            new PlayerShiftingObjective(
                new Objective("PlayerShifting", "PlayerShifting", "PlayerShifting")),
            new PotionConsumeObjective(
                new Objective("PotionConsume", "PotionConsume", "PotionConsume")),
            new QuickKillsObjective(new Objective("QuickKills", "QuickKills", "QuickKills")),
            new HedgehogObjective(new Objective("Hedgehog", "Hedgehog", "Hedgehog")),
            new ArmourSharedObjective(new Objective("ArmourShared", "ArmourShared", "ArmourShared")))


            );
  }
}
