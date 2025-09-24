package tc.oc.bingo.objectives;

import tc.oc.bingo.modules.DependsOn;
import tc.oc.bingo.modules.GravesModule;

@Tracker("grave-punch")
@DependsOn(GravesModule.class)
public class GravePunchObjective extends ObjectiveTracker {}
