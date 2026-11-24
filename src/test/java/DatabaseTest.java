import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/** Tests JDBC transaction behavior with an in-memory driver, not a MySQL server. */
public final class DatabaseTest {
    private static int checks;
    private static void check(boolean test, String message) {
        // Assertion counter is printed as evidence for the database defense.
        checks++;
        if (!test) throw new AssertionError(message);
    }

    private static List<Arceus.PokemonData> team(int attack) {
        // Use six simple members so persistence tests focus on rows and transactions.
        List<Arceus.PokemonData> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) result.add(new Arceus.PokemonData(
                i + 1, "Member " + i, "Normal", 100, attack, attack, "One", "Two", 1.0));
        return result;
    }

    private static BattleLogic winner() {
        // Create a completed WON run without opening Swing or connecting to MySQL.
        BattleLogic battle = new BattleLogic(team(1000),
                List.of(team(1), team(1), team(1), team(1)));
        for (int stage = 0; stage < 4; stage++) {
            for (int member = 0; member < 6; member++) battle.attack(2);
            if (stage < 3) battle.nextStage(true);
        }
        return battle;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        // Dynamic proxies provide an in-memory JDBC driver with no external database dependency.
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static final class Fixture implements Driver {
        // This fixture records SQL effects and can inject batch or commit failures.
        final Map<String, List<String>> wins = new LinkedHashMap<>();
        Properties lastConnectionSettings;
        boolean failBatch, failCommitAcknowledgement;
        int rollbacks;

        public Connection connect(String url, Properties properties) {
            if (!acceptsURL(url)) return null;
            lastConnectionSettings = new Properties();
            lastConnectionSettings.putAll(properties);
            String[] pending = {null};
            List<String> members = new ArrayList<>();
            return proxy(Connection.class, (obj, method, args) -> {
                switch (method.getName()) {
                    case "prepareStatement":
                        // Match the SQL operations used by Database without running a real server.
                        String sql = (String) args[0];
                        Map<Integer, Object> parameters = new HashMap<>();
                        List<String> batch = new ArrayList<>();
                        return proxy(PreparedStatement.class, (statement, action, values) -> {
                            switch (action.getName()) {
                                case "setString": case "setInt":
                                    parameters.put((Integer) values[0], values[1]); return null;
                                case "executeUpdate":
                                    String run = (String) parameters.get(1);
                                    if (wins.containsKey(run)) throw new SQLIntegrityConstraintViolationException(
                                            "Duplicate run", "23000", 1062);
                                    pending[0] = run; return 1;
                                case "addBatch": batch.add((String) parameters.get(4)); return null;
                                case "executeBatch":
                                    if (failBatch) throw new SQLException("Injected team insert failure");
                                    members.addAll(batch);
                                    int[] counts = new int[batch.size()];
                                    Arrays.fill(counts, 1); return counts;
                                case "executeQuery":
                                    if (sql.startsWith("SELECT w.run_id,")) {
                                        List<String> runs = new ArrayList<>(wins.keySet());
                                        Collections.reverse(runs);
                                        int offset = (Integer) parameters.get(2);
                                        int end = Math.min(runs.size(), offset + (Integer) parameters.get(1));
                                        List<Map<String, Object>> records = new ArrayList<>();
                                        for (int i = offset; i < end; i++) {
                                            String id = runs.get(i);
                                            List<String> names = wins.get(id);
                                            for (int slot = 0; slot < names.size(); slot++) {
                                                records.add(Map.of("run_id", id,
                                                        "won_at", Timestamp.valueOf("2026-11-22 12:00:00"),
                                                        "slot_number", slot + 1,
                                                        "pokemon_id", slot + 1,
                                                        "pokemon_name", names.get(slot)));
                                            }
                                        }
                                        int[] cursor = {-1};
                                        return proxy(ResultSet.class, (rows, read, fields) -> {
                                            if (read.getName().equals("next")) return ++cursor[0] < records.size();
                                            if (read.getName().startsWith("get")) {
                                                return records.get(cursor[0]).get((String) fields[0]);
                                            }
                                            return defaultValue(read.getReturnType());
                                        });
                                    }
                                    boolean exists = sql.contains("COUNT(*)")
                                            || wins.containsKey((String) parameters.get(1));
                                    boolean[] visited = {false};
                                    return proxy(ResultSet.class, (rows, read, fields) -> {
                                        if (read.getName().equals("next")) {
                                            boolean available = exists && !visited[0];
                                            visited[0] = true; return available;
                                        }
                                        if (read.getName().equals("getLong")) return (long) wins.size();
                                        return defaultValue(read.getReturnType());
                                    });
                                default: return defaultValue(action.getReturnType());
                            }
                        });
                    case "commit":
                        // Apply pending rows only at commit, which lets rollback tests detect partial saves.
                        if (pending[0] != null) wins.put(pending[0], List.copyOf(members));
                        pending[0] = null;
                        if (failCommitAcknowledgement) {
                            failCommitAcknowledgement = false;
                            throw new SQLException("Injected lost commit response");
                        }
                        return null;
                    case "rollback": rollbacks++; pending[0] = null; members.clear(); return null;
                    default: return defaultValue(method.getReturnType());
                }
            });
        }
        public boolean acceptsURL(String url) { return url.startsWith("jdbc:mysql:"); }
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        public int getMajorVersion() { return 1; }
        public int getMinorVersion() { return 0; }
        public boolean jdbcCompliant() { return false; }
        public Logger getParentLogger() { return Logger.getGlobal(); }
    }

    public static void main(String[] args) throws Exception {
        // Register the fixture, exercise save/retry/rollback/pagination, then deregister it.
        Fixture fixture = new Fixture();
        DriverManager.registerDriver(fixture);
        try {
            Database db = new Database();
            BattleLogic first = winner();
            check(db.recordVictory(first) == 1, "First completed run increments wins");
            // Verify connection settings and the six saved team members.
            check("pokemon_app".equals(fixture.lastConnectionSettings.getProperty("user")),
                    "Connection uses the supplied hardcoded account");
            check(!fixture.lastConnectionSettings.getProperty("password", "").isEmpty(),
                    "Connection uses a nonempty supplied password rather than the old blank fallback");
            check("5000".equals(fixture.lastConnectionSettings.getProperty("connectTimeout")),
                    "Connection retains its timeout");
            check(fixture.wins.get(first.getRunId()).size() == 6, "All six species are saved");
            check(db.recordVictory(first) == 1, "Retry does not add another win");
            fixture.failBatch = true;
            // A team insert failure must roll back the victory rather than leave a partial record.
            BattleLogic second = winner();
            try { db.recordVictory(second); throw new AssertionError("Failure was swallowed"); }
            catch (SQLException expected) { checks++; }
            check(fixture.wins.size() == 1, "Failed team insert leaves no partial victory");
            fixture.failBatch = false;
            check(db.recordVictory(second) == 2, "Retry after failure saves once");
            fixture.failCommitAcknowledgement = true;
            // A lost commit response must be safe to retry using the same run UUID.
            BattleLogic third = winner();
            try { db.recordVictory(third); throw new AssertionError("Commit failure was swallowed"); }
            catch (SQLException expected) { checks++; }
            check(db.recordVictory(third) == 3, "Lost commit acknowledgement can be retried without duplication");
            BattleLogic active = new BattleLogic(team(100), List.of(team(1), team(1), team(1), team(1)));
            try { db.recordVictory(active); throw new AssertionError("Incomplete run recorded"); }
            catch (IllegalStateException expected) { checks++; }
            check(fixture.rollbacks >= 3, "Failure paths roll back");
            List<Database.HallOfFameEntry> history = db.getHallOfFame(0, 2);
            // Pagination is by victory, not by individual team member rows.
            check(history.size() == 2, "History page limits victories rather than individual Pokémon");
            check(history.get(0).runId().equals(third.getRunId()), "Newest victory is listed first");
            check(history.get(0).team().size() == 6, "History includes the complete six-member team");
            check(history.get(0).team().get(0).slot() == 1
                    && history.get(0).team().get(5).name().equals("Member 5"),
                    "History preserves selection order and saved names");
            check(history.get(0).wonAt().getYear() == 2026, "History retrieves victory timestamp");
            check(db.getHallOfFame(2, 2).get(0).runId().equals(first.getRunId()),
                    "Next history page retrieves remaining victories");
            check(db.getHallOfFame(3, 2).isEmpty(), "History beyond the final page is empty");
            try { db.getHallOfFame(-1, 10); throw new AssertionError("Negative offset accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
            check(db.getWinCount() == 3, "Viewing history never increments wins");
            System.out.println("PASS: " + checks + " JDBC fixture checks (not a live MySQL test).");
        } finally {
            DriverManager.deregisterDriver(fixture);
        }
    }
}
