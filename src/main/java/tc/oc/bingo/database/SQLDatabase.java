package tc.oc.bingo.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nullable;
import lombok.Cleanup;
import lombok.SneakyThrows;
import tc.oc.bingo.util.ExceptionHandlingExecutorService;
import tc.oc.occ.database.Database;

public class SQLDatabase implements BingoDatabase {

  private static final String BINGO_OBJECTIVES = "bingo_objectives";
  private static final String BINGO_PROGRESS = "bingo_progress";

  public static final String CREATE_OBJECTIVE_TABLE_SQL =
      "CREATE TABLE IF NOT EXISTS "
          + BINGO_OBJECTIVES
          + " ("
          + "slug VARCHAR(255) PRIMARY KEY,"
          + "name VARCHAR(255),"
          + "description TEXT,"
          + "idx INT,"
          + "clue TEXT,"
          + "hint_level INT,"
          + "next_clue_unlock DATETIME,"
          + "discovery_uuid VARCHAR(255),"
          + "discovery_time DATETIME"
          + ")";

  public static final String CREATE_PROGRESS_TABLE_SQL =
      "CREATE TABLE IF NOT EXISTS "
          + BINGO_PROGRESS
          + " ("
          + "player_uuid VARCHAR(255),"
          + "objective_slug VARCHAR(255),"
          + "completed BOOLEAN DEFAULT FALSE,"
          + "completed_at DATETIME,"
          + "placed_position INT,"
          + "data TEXT,"
          + "PRIMARY KEY (player_uuid, objective_slug)"
          + ")";

  private static final String PROGRESS_COUNT_QUERY =
      "SELECT COUNT(*) FROM " + BINGO_PROGRESS + " WHERE objective_slug = ? AND completed";

  private static final String UPSERT_COMPLETED_SQL =
      "INSERT INTO "
          + BINGO_PROGRESS
          + " (player_uuid, objective_slug, completed, completed_at, placed_position) "
          + "VALUES (?, ?, ?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE completed = VALUES(completed), completed_at = VALUES(completed_at), placed_position = VALUES(placed_position)";

  private static final String UPSERT_PROGRESS_SQL =
      "INSERT INTO "
          + BINGO_PROGRESS
          + " (player_uuid, objective_slug, data) "
          + "VALUES (?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE data = VALUES(data)";

  private static final String UPDATE_OBJECTIVE_DISCOVERY_SQL =
      "UPDATE " + BINGO_OBJECTIVES + " SET discovery_uuid = ?, discovery_time = ? WHERE slug = ?";

  private static final ExceptionHandlingExecutorService EXECUTOR =
      new ExceptionHandlingExecutorService(ForkJoinPool.commonPool());;

  public SQLDatabase() {
    createTables();
  }

  private Connection getConnection() throws SQLException {
    return Database.get().getConnectionPool().getPool().getConnection();
  }

  private void createTables() {
    CompletableFuture.runAsync(
        () -> {
          createTablesImpl(CREATE_OBJECTIVE_TABLE_SQL);
          createTablesImpl(CREATE_PROGRESS_TABLE_SQL);
        },
        EXECUTOR);
  }

  @SneakyThrows
  public void createTablesImpl(String sqlQuery) {
    @Cleanup Connection conn = getConnection();
    @Cleanup PreparedStatement stmt = conn.prepareStatement(sqlQuery);
    stmt.execute();
  }

  @Override
  public CompletableFuture<BingoCard> getCard() {
    return CompletableFuture.supplyAsync(
        () -> {
          // Opening database connection
          try (Connection connection = getConnection()) {

            List<ObjectiveItem> objectives = new ArrayList<>();

            // SQL query to retrieve user's objective progress
            String sql = "SELECT * FROM " + BINGO_OBJECTIVES + " WHERE idx != -1";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            // Executing query
            ResultSet resultSet = null;
            resultSet = preparedStatement.executeQuery();

            // Processing results
            while (resultSet.next()) {

              String slug = resultSet.getString("slug");
              String name = resultSet.getString("name");
              String description = resultSet.getString("description");
              int index = resultSet.getInt("idx");
              String clue = resultSet.getString("clue");
              int hintLevel = resultSet.getInt("hint_level");

              Timestamp nextClueUnlockTimestamp = resultSet.getTimestamp("next_clue_unlock");
              LocalDateTime nextClueUnlockTime =
                  (nextClueUnlockTimestamp != null)
                      ? nextClueUnlockTimestamp.toLocalDateTime()
                      : null;

              String discoveryString = resultSet.getString("discovery_uuid");
              UUID discoveryUuid = resultSet.wasNull() ? null : UUID.fromString(discoveryString);

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
        },
        EXECUTOR);
  }

  @Override
  public CompletableFuture<BingoPlayerCard> getCard(UUID playerId) {

    return CompletableFuture.supplyAsync(
        () -> {
          // Opening database connection
          try (Connection connection = getConnection()) {

            Map<String, ProgressItem> progressList = new HashMap<>();

            // SQL query to retrieve user's objective progress
            String sql = "SELECT * FROM " + BINGO_PROGRESS + " WHERE player_uuid = ?";
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
            }

            return new BingoPlayerCard(progressList);

          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        },
        EXECUTOR);
  }

  public CompletableFuture<Integer> getCompletionCount(String objectiveSlug) {
    // Check the database to see how many other players have completed this task by slug
    return CompletableFuture.supplyAsync(() -> getCompletionCountImpl(objectiveSlug), EXECUTOR);
  }

  @SneakyThrows
  private int getCompletionCountImpl(String objectiveSlug) {
    @Cleanup Connection conn = getConnection();
    @Cleanup PreparedStatement stmt = conn.prepareStatement(PROGRESS_COUNT_QUERY);

    stmt.setString(1, objectiveSlug);

    ResultSet resultSet = stmt.executeQuery();
    int numCompleted = 0;
    if (resultSet.next()) {
      numCompleted = resultSet.getInt(1);
    }

    return numCompleted;
  }

  @Override
  public CompletableFuture<Integer> rewardPlayers(List<UUID> players, String objectiveSlug) {
    return getCompletionCount(objectiveSlug)
        .thenApply(
            position -> {
              // Add one to this number and upsert an entry for storing completion
              position++;
              storePlayerCompletion(players, objectiveSlug, position);

              if (position == 1) {
                UUID playerUUID = players.size() == 1 ? players.get(0) : null;
                storeGoalDiscoverer(playerUUID, objectiveSlug);
              }

              return position;
            });
  }

  @Override
  public CompletableFuture<Void> storePlayerProgress(
      UUID playerId, String objectiveSlug, String dataAsString) {
    return CompletableFuture.runAsync(
        () -> storePlayerProgressImpl(playerId, objectiveSlug, dataAsString), EXECUTOR);
  }

  @SneakyThrows
  public void storePlayerProgressImpl(UUID playerId, String objectiveSlug, String dataAsString) {
    @Cleanup Connection conn = getConnection();
    @Cleanup PreparedStatement stmt = conn.prepareStatement(UPSERT_PROGRESS_SQL);
    stmt.setString(1, playerId.toString());
    stmt.setString(2, objectiveSlug);
    stmt.setString(3, dataAsString);
    stmt.executeUpdate();
  }

  @Override
  public CompletableFuture<Void> storePlayerCompletion(
      List<UUID> uuids, String objectiveSlug, Integer position) {
    return CompletableFuture.runAsync(
        () -> storePlayerCompletionsImpl(uuids, objectiveSlug, position), EXECUTOR);
  }

  @SneakyThrows
  private void storePlayerCompletionsImpl(
      List<UUID> uuids, String objectiveSlug, Integer position) {
    @Cleanup Connection conn = getConnection();
    @Cleanup PreparedStatement stmt = conn.prepareStatement(UPSERT_COMPLETED_SQL);

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    boolean isBatch = uuids.size() > 1;

    for (UUID uuid : uuids) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, objectiveSlug);
      stmt.setBoolean(3, true);
      stmt.setTimestamp(4, timestamp);
      stmt.setInt(5, position);

      if (isBatch) stmt.addBatch();
    }

    if (isBatch) stmt.executeBatch();
    else stmt.executeUpdate();
  }

  @Override
  public CompletableFuture<Void> storeGoalDiscoverer(@Nullable UUID uuid, String objectiveSlug) {
    return CompletableFuture.runAsync(() -> storeGoalDiscovererImpl(uuid, objectiveSlug), EXECUTOR);
  }

  @SneakyThrows
  public void storeGoalDiscovererImpl(@Nullable UUID uuid, String objectiveSlug) {
    @Cleanup Connection conn = getConnection();
    @Cleanup PreparedStatement stmt = conn.prepareStatement(UPDATE_OBJECTIVE_DISCOVERY_SQL);

    stmt.setString(1, (uuid != null) ? uuid.toString() : null);
    stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
    stmt.setString(3, objectiveSlug);
    stmt.executeUpdate();
  }
}
