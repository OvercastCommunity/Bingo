package tc.oc.bingo.card;

import javax.annotation.Nullable;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public enum RewardType {
  NONE("None", null),
  SINGLE("Single", null),
  LINE("Line", NamedTextColor.GOLD),
  CARD("Full House", NamedTextColor.GOLD);

  private final String name;
  private final boolean broadcast;
  private final NamedTextColor color;

  RewardType(String name, @Nullable NamedTextColor color) {
    this.name = name;
    this.broadcast = color != null;
    this.color = color;
  }
}
