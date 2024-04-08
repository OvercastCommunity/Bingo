package tc.oc.bingo.util;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerResolver;
import tc.oc.pgm.api.player.MatchPlayerState;

public interface PGMUtils extends MatchPlayerResolver {

  default @Nullable Match getMatch(World world) {
    return PGM.get().getMatchManager().getMatch(world);
  }

  default @Nullable MatchPlayer getPlayer(@Nullable Player player) {
    return player == null ? null : PGM.get().getMatchManager().getPlayer(player);
  }

  default @Nullable MatchPlayer getStatePlayer(@Nullable MatchPlayerState player) {
    return player == null ? null : player.getPlayer().orElse(null);
  }
}
