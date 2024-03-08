package tc.oc.bingo.objectives;

import tc.oc.pgm.api.player.MatchPlayer;

public class Objective {

  public String name;
  public String description;
  public String slug;

  public boolean completed;

  public Objective(String name, String description, String slug) {
    this.name = name;
    this.description = description;
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getSlug() {
    return slug;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void reward(MatchPlayer player) {}
}
