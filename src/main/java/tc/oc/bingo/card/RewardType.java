package tc.oc.bingo.card;

import lombok.Getter;

@Getter
public enum RewardType {
  NONE("None", false),
  SINGLE("Single", false),
  LINE("Line", true),
  CARD("Full House", true);

  private final String name;
  private final boolean broadcast;

  RewardType(String name, boolean broadcast) {
    this.name = name;
    this.broadcast = broadcast;
  }
}
