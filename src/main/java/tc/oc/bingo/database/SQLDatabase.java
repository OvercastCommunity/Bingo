package tc.oc.bingo.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.occ.database.Database;

public class SQLDatabase implements BingoDatabase {

  private static final int MAX_THREADS = 2;

  private static final String BINGO_LIFETIME_TABLE = "raindrop_lifetime";
  private static final String LIFETIME_TABLE = "raindrop_lifetime";

  public SQLDatabase() {
    createTables();
  }

  private Connection getConnection() throws SQLException {
    return Database.get().getConnectionPool().getPool().getConnection();
  }

  private void createTables() {

    try {
      // Opening database connection
      Connection connection = getConnection();

      // Creating statement
      Statement statement = connection.createStatement();

      // Creating Objective table
      String createObjectiveTableSQL =
          "CREATE TABLE IF NOT EXISTS bingo_objectives ("
              + "slug VARCHAR(255) PRIMARY KEY,"
              + "name VARCHAR(255),"
              + "description TEXT,"
              + "index INT,"
              + "clue TEXT,"
              + "hint_level INT,"
              + "next_clue_unlock DATETIME,"
              + "discovery_uuid VARCHAR(255),"
              + "discovery_time DATETIME"
              + ")";

      statement.execute(createObjectiveTableSQL);
      System.out.println("Objective table created successfully.");

      // Creating Bingo Progress table
      String createBingoProgressTableSQL =
          "CREATE TABLE IF NOT EXISTS bingo_progress ("
              + "player_uuid VARCHAR(255),"
              + "objective_slug VARCHAR(255),"
              + "completed BOOLEAN DEFAULT FALSE,"
              + "completed_at DATETIME"
              + "placed_position INT,"
              + "data JSON,"
              + "PRIMARY KEY (player_uuid, objective_slug)"
              + ")";

      statement.execute(createBingoProgressTableSQL);
      System.out.println("Bingo Progress table created successfully.");

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public CompletableFuture<BingoCard> getCard() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {

            // Opening database connection
            Connection connection = getConnection();

            List<ObjectiveItem> objectives = new ArrayList<>();

            // SQL query to retrieve user's objective progress
            String sql = "SELECT * FROM bingo_objectives";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            // Executing query
            ResultSet resultSet = null;
            resultSet = preparedStatement.executeQuery();

            // Processing results
            while (resultSet.next()) {

              String slug = resultSet.getString("slug");
              String name = resultSet.getString("name");
              String description = resultSet.getString("description");
              int index = resultSet.getInt("index");
              String clue = resultSet.getString("clue");
              int hintLevel = resultSet.getInt("hintLevel");

              Timestamp nextClueUnlockTimestamp = resultSet.getTimestamp("next_clue_unlock");
              LocalDateTime nextClueUnlockTime =
                  (nextClueUnlockTimestamp != null)
                      ? nextClueUnlockTimestamp.toLocalDateTime()
                      : null;

              String discoveryUuid = resultSet.getString("discovery_uuid");

              Timestamp discoveryTimestamp = resultSet.getTimestamp("discovery_time");
              LocalDateTime discoveryTime =
                  (discoveryTimestamp != null) ? discoveryTimestamp.toLocalDateTime() : null;

              ObjectiveItem objective =
                  new ObjectiveItem(
                      slug,
                      name,
                      description,
                      index,
                      clue,
                      hintLevel,
                      nextClueUnlockTime,
                      discoveryUuid,
                      discoveryTime);
              objectives.add(objective);
            }

            return new BingoCard(objectives);

          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public CompletableFuture<BingoPlayerCard> getCard(UUID playerId) {

    return CompletableFuture.supplyAsync(
        () -> {
          try {

            // Opening database connection
            Connection connection = getConnection();

            HashMap<String, ProgressItem> progressList = new HashMap<>();

            // SQL query to retrieve user's objective progress
            String sql = "SELECT * FROM bingo_progress WHERE player_uuid = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, playerId.toString());

            // Executing query
            ResultSet resultSet = null;
            resultSet = preparedStatement.executeQuery();

            // Processing results
            while (resultSet.next()) {
              String objectiveSlug = resultSet.getString("objective_slug");
              boolean completed = resultSet.getBoolean("completed");
              Integer placedPosition =
                  resultSet.wasNull() ? null : resultSet.getInt("placed_position");
              String data = resultSet.getString("data");

              ProgressItem progress =
                  new ProgressItem(playerId, objectiveSlug, completed, placedPosition, data);
              progressList.put(objectiveSlug, progress);

              // Printing user's objective progress
              System.out.println("Objective Slug: " + objectiveSlug);
              System.out.println("Completed: " + completed);
              System.out.println("Placed Position: " + placedPosition);
              System.out.println("Data: " + data);
              System.out.println("---------------------------------");
            }

            return new BingoPlayerCard(progressList);

          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public void updateObjective(UUID playerId, int amount) {}
}
