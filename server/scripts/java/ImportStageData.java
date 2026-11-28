import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit one-time import of the former frontend records; never run at application startup. */
public class ImportStageData {
    private static final String[][] ACCOUNTS = {
        {"resident", "邻居小林", "RESIDENT", "3", "2", "501"},
        {"neighbor", "邻居小叶", "RESIDENT", "5", "1", "302"},
        {"worker", "网格员小周", "STAFF", "2", "1", "101"},
        {"admin", "社区小陈", "ADMIN", "1", "1", "101"},
        {"authorized", "住户服务员", "ADMIN", "1", "1", "102"}
    };

    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(args[0]))) { config.load(reader); }
        var encoder = new BCryptPasswordEncoder(12);
        try (var db = DriverManager.getConnection(config.getProperty("url"), config.getProperty("user"), config.getProperty("password"))) {
            db.setAutoCommit(false);
            try {
                int existing = 0;
                for (String[] account : ACCOUNTS) {
                    try (var query = db.prepareStatement("SELECT `角色`,`密码摘要` FROM `用户账号` WHERE `用户名`=?")) {
                        query.setString(1, account[0]);
                        try (var rows = query.executeQuery()) {
                            if (!rows.next()) continue;
                            if (!account[2].equals(rows.getString(1)) || !encoder.matches(config.getProperty(account[0]), rows.getString(2)))
                                throw new IllegalStateException("Existing account will not be overwritten: " + account[0]);
                            existing++;
                        }
                    }
                }
                if (existing != 0) {
                    // A complete import is committed atomically. Never repair or reset edited records here.
                    if (existing != ACCOUNTS.length) throw new IllegalStateException("Partial/name-conflicting data; manual review required");
                    try (var s = db.createStatement(); var rows = s.executeQuery("SELECT COUNT(*) FROM `社区活动` WHERE `活动编号`='cefa0000-0000-4000-8000-000000000001'")) {
                        rows.next();
                        if (rows.getInt(1) != 1) throw new IllegalStateException("Import marker absent; refusing duplicate initialization");
                    }
                    db.rollback();
                    System.out.println("Already imported; existing records unchanged.");
                    return;
                }
                for (String[] account : ACCOUNTS) {
                    String password = config.getProperty(account[0]);
                    if (password == null || password.length() < 16) throw new IllegalArgumentException("Missing generated credential");
                    try (var insert = db.prepareStatement("INSERT INTO `用户账号` (`用户名`,`密码摘要`,`昵称`,`角色`) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        insert.setString(1, account[0]); insert.setString(2, encoder.encode(password));
                        insert.setString(3, account[1]); insert.setString(4, account[2]); insert.executeUpdate();
                        try (var keys = insert.getGeneratedKeys()) {
                            if (!keys.next()) throw new IllegalStateException("Account key missing");
                            try (var profile = db.prepareStatement("INSERT INTO `居民资料` (`用户编号`,`楼栋`,`单元`,`门牌`) VALUES (?,?,?,?)")) {
                                profile.setLong(1, keys.getLong(1));
                                for (int i = 3; i < 6; i++) profile.setString(i - 1, account[i]);
                                profile.executeUpdate();
                            }
                        }
                    }
                }
                String sql = Files.readString(Path.of(args[1])).replaceAll("(?m)^--.*$", "");
                try (var statement = db.createStatement()) {
                    for (String command : sql.split(";")) if (!command.isBlank()) statement.execute(command);
                }
                db.commit();
                System.out.println("Imported: 5 accounts (2 residents), 3 venues, 1 order/4 logs, 5 activities, 1 enrollment, 2 interests, 1 permission.");
            } catch (Exception error) { db.rollback(); throw error; }
        }
    }
}
