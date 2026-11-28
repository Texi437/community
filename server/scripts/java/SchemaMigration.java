import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;

/** Versioned schema migration with full-state verification and durable failure records. */
public class SchemaMigration {
    private static final String LEDGER = """
        CREATE TABLE schema_migration (
          `迁移版本` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
          script_name VARCHAR(200) NOT NULL,
          checksum CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
          status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
          started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
          finished_at TIMESTAMP(6) NULL,
          CONSTRAINT `迁移状态范围` CHECK(status IN('RUNNING','SUCCESS','FAILED'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
        """;
    private record Migration(String version, String name, String checksum, String sql, List<String> tables) {}
    public static void main(String[] args) throws Exception {
        if (args.length!=4) throw new IllegalArgumentException("Usage: SchemaMigration <port> <database> <sql-directory> <check|apply>");
        String db=args[1], mode=args[3];
        if (!db.matches("linli_[a-z0-9_]+") || !Set.of("check","apply").contains(mode))
            throw new IllegalArgumentException("Invalid database or mode");
        List<Migration> migrations = readMigrations(Path.of(args[2]));
        String password=System.getenv("LINLI_MIGRATION_PASSWORD");
        if(password==null) throw new IllegalStateException("Missing migration credential");
        String url="jdbc:mysql://127.0.0.1:"+Integer.parseInt(args[0])+"/?characterEncoding=UTF-8&connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&sslMode=DISABLED&allowPublicKeyRetrieval=true";
        try(Connection c=DriverManager.getConnection(url,"root",password)) {
            String lock="linli-schema:"+db;
            try(PreparedStatement p=c.prepareStatement("SELECT GET_LOCK(?,0)")) {
                p.setString(1,lock);
                try(ResultSet rs=p.executeQuery()) { rs.next(); if(rs.getInt(1)!=1) throw new IllegalStateException("Another migration holds the lock"); }
            }
            String reference="linli_schema_ref_"+UUID.randomUUID().toString().replace("-","");
            boolean created=false;
            try {
                c.setCatalog(db);
                run(c,"SET SESSION lock_wait_timeout=15");
                Map<String,String> initial=definitions(c);
                int complete=initial.containsKey("schema_migration") ? verifyLedger(c,migrations) : 0;
                // Historical checksums/status are checked before any reference SQL executes.
                run(c,"CREATE DATABASE "+reference+" CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"); created=true;
                c.setCatalog(reference);
                run(c,LEDGER);
                List<Map<String,String>> snapshots=new ArrayList<>();
                snapshots.add(definitions(c));
                for(Migration m:migrations) {
                    int referenceStep=0;
                    for(String sql:statements(m.sql)) {
                        referenceStep++;
                        try {run(c,sql);}
                        catch(SQLException ex) {throw new SQLException("Reference "+m.version+" statement "+referenceStep+": "+ex.getMessage(),ex);}
                    }
                    snapshots.add(definitions(c));
                }
                c.setCatalog(db);
                boolean baseline=false;
                if(!initial.isEmpty()) {
                    if(initial.containsKey("schema_migration")) {
                        if(complete==0) throw new IllegalStateException("Empty migration ledger requires inspection");
                        requireSchema(snapshots.get(complete),initial);
                    } else {
                        Map<String,String> legacy=new TreeMap<>(snapshots.get(2));
                        legacy.remove("schema_migration");
                        requireSchema(legacy,initial);
                        complete=2; baseline=true;
                    }
                }
                if(mode.equals("check")) {
                    System.out.println("Baseline check passed; target tables="+initial.size()+"; completed="+complete+"; no target writes.");
                    return;
                }
                if(db.equals("linli_governance") && complete<7) requireBackendStopped();
                if(!initial.containsKey("schema_migration")) {
                    run(c,LEDGER);
                    if(baseline) for(int i=0;i<2;i++) record(c,migrations.get(i),"SUCCESS");
                }
                for(int i=0;i<migrations.size();i++) {
                    Migration m=migrations.get(i);
                    if(i<complete) {System.out.println("Verified, skipped "+m.version);continue;}
                    // Full version snapshots handle renamed tables without comparing V007 to obsolete V006 names.
                    requireSchema(snapshots.get(i),definitions(c));
                    record(c,m,"RUNNING");
                    int statement=0;
                    try {
                        for(String sql:statements(m.sql)) {statement++;run(c,sql);}
                        requireSchema(snapshots.get(i+1),definitions(c));
                        finish(c,m.version,"SUCCESS");
                        System.out.println("Applied "+m.version);
                    } catch(Exception failure) {
                        finish(c,m.version,"FAILED");
                        System.err.println("Migration "+m.version+" failed at statement "+statement+"; preserve backup and inspect partial DDL before recovery.");
                        throw failure;
                    }
                }
                requireSchema(snapshots.get(migrations.size()),definitions(c));
                System.out.println("Schema verified: "+snapshots.get(migrations.size()).size()+" tables at "+Instant.now());
            } finally {
                // Only the random reference created by this invocation is eligible for cleanup.
                if(created && reference.matches("linli_schema_ref_[a-f0-9]{32}")) {
                    c.setCatalog("information_schema"); run(c,"DROP DATABASE "+reference);
                }
                try(PreparedStatement p=c.prepareStatement("SELECT RELEASE_LOCK(?)")) { p.setString(1,lock); p.execute(); }
            }
        }
    }
    private static List<Migration> readMigrations(Path dir) throws Exception {
        List<Path> paths;
        try(var stream=Files.list(dir)) { paths=stream.filter(p->p.getFileName().toString().matches("V[0-9]{3}__.+\\.sql")).sorted().toList(); }
        List<Migration> result=new ArrayList<>();
        Pattern create=Pattern.compile("(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z_]+)");
        for(Path p:paths) {
            byte[] bytes=Files.readAllBytes(p);
            String sql=new String(bytes,StandardCharsets.UTF_8);
            List<String> tables=new ArrayList<>();
            for(String statement:statements(sql)) {
                // None of our additive schema statements require qualification or decimal literals.
                // Dots outside SQL string literals could target another schema; reject them before execution.
                if(statement.replaceAll("'(''|[^'])*'", "''").contains("."))
                    throw new IllegalStateException("Qualified SQL identifiers are not allowed in "+p.getFileName());
                if(p.getFileName().toString().startsWith("V007__")) {
                    validateRename(statement);
                    continue;
                }
                Matcher matcher=create.matcher(statement);
                if(matcher.find() && matcher.start()==0 && statement.substring(matcher.end()).stripLeading().startsWith("(")) tables.add(matcher.group(1));
                else if(!statement.matches("(?is)INSERT\\s+INTO\\s+interest_topic\\s+.*"))
                    throw new IllegalStateException("Unexpected non-additive statement in "+p.getFileName());
            }
            String name=p.getFileName().toString();
            result.add(new Migration(name.substring(0,4),name,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),sql,tables));
        }
        if(!result.stream().map(Migration::version).toList().equals(List.of("V001","V002","V003","V004","V005","V006","V007")))
            throw new IllegalStateException("Expected V001 through V007 exactly");
        return result;
    }
    private static void validateRename(String sql) {
        String old="`[a-z_]+`", id="`[a-z_\\p{IsHan}]+`", chinese="`[\\p{IsHan}]+`";
        // V007 is a narrowly scoped rename, never arbitrary ALTER/DROP/UPDATE.
        if(sql.matches("(?s)RENAME TABLE\\s+"+old+" TO "+chinese+"(?:,\\s*"+old+" TO "+chinese+")*")) return;
        Matcher m=Pattern.compile("(?s)ALTER TABLE ("+old+")\\s+(.+)").matcher(sql);
        if(!m.matches() || m.group(1).equals("`schema_migration`")) throw new IllegalStateException("Unexpected V007 statement");
        for(String clause:m.group(2).split(",\\s*(?=RENAME |DROP |ADD |ALGORITHM=)")) {
            if(clause.equals("ALGORITHM=INPLACE")) continue;
            if(clause.matches("RENAME (?:COLUMN|INDEX) "+id+" TO "+chinese)) continue;
            if(clause.matches("DROP CHECK "+id)) continue;
            if(clause.matches("(?s)ADD CONSTRAINT "+chinese+" CHECK \\(.+\\)")) continue;
            throw new IllegalStateException("Unexpected V007 clause: "+clause);
        }
    }
    private static void requireBackendStopped() throws Exception {
        int port=Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT","8080"));
        try(java.net.Socket socket=new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1",port),1000);
            throw new IllegalStateException("Stop the backend on port "+port+" before V007; restart only with the matching Chinese-schema build.");
        } catch(java.net.ConnectException stopped) {
            // Refused connection is the expected maintenance state. Other I/O failures remain fatal.
        }
    }
    private static List<String> statements(String input) {
        String sql=input.replace("\uFEFF","").replaceAll("(?m)^\\s*--[^\\r\\n]*","");
        List<String> out=new ArrayList<>(); StringBuilder current=new StringBuilder(); char quote=0;
        for(int i=0;i<sql.length();i++) {
            char ch=sql.charAt(i);
            if(quote!=0) {
                current.append(ch);
                if(ch=='\\' && i+1<sql.length()) current.append(sql.charAt(++i));
                else if(ch==quote) {
                    if(i+1<sql.length() && sql.charAt(i+1)==quote) current.append(sql.charAt(++i));
                    else quote=0;
                }
            } else if(ch=='\''||ch=='"'||ch=='`') { quote=ch; current.append(ch); }
            else if(ch==';') { if(!current.toString().isBlank()) out.add(current.toString().strip()); current.setLength(0); }
            else current.append(ch);
        }
        if(quote!=0) throw new IllegalArgumentException("Unclosed SQL quote");
        if(!current.toString().isBlank()) out.add(current.toString().strip());
        return out;
    }
    private static Map<String,String> definitions(Connection c) throws SQLException {
        List<String> names=new ArrayList<>();
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SHOW FULL TABLES")) {
            while(rs.next()) {
                if(!"BASE TABLE".equals(rs.getString(2))) throw new IllegalStateException("Unsupported schema object: "+rs.getString(1));
                names.add(rs.getString(1));
            }
        }
        Map<String,String> out=new TreeMap<>();
        for(String n:names) {
            if(!n.matches("[a-z_\\p{IsHan}]+")) throw new IllegalStateException("Unexpected table identifier");
            try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SHOW CREATE TABLE `"+n+"`")) {
                rs.next(); out.put(n,rs.getString(2).replaceAll(" AUTO_INCREMENT=\\d+",""));
            }
        }
        return out;
    }
    private static void requireSchema(Map<String,String> expected,Map<String,String> actual) {
        if(!expected.keySet().equals(actual.keySet())) throw new IllegalStateException("Schema table set mismatch; target unchanged by this check");
        for(String table:expected.keySet()) if(!expected.get(table).equals(actual.get(table)))
            throw new IllegalStateException("Schema mismatch: "+table+"; target unchanged by this check");
    }
    private static int verifyLedger(Connection c,List<Migration> migrations) throws SQLException {
        int completed=0;
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT `迁移版本`,script_name,checksum,status FROM schema_migration ORDER BY `迁移版本`")) {
            while(rs.next()) {
                if(completed>=migrations.size()) throw new IllegalStateException("Unknown migration version");
                Migration m=migrations.get(completed);
                if(!m.version.equals(rs.getString(1))||!m.name.equals(rs.getString(2))||!m.checksum.equals(rs.getString(3)))
                    throw new IllegalStateException("Migration sequence/checksum/name mismatch: "+rs.getString(1));
                if(!"SUCCESS".equals(rs.getString(4))) throw new IllegalStateException("Unfinished migration requires inspection: "+m.version);
                completed++;
            }
        }
        return completed;
    }
    private static void record(Connection c,Migration m,String status) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO schema_migration(`迁移版本`,script_name,checksum,status,finished_at) VALUES(?,?,?,?,?)")) {
            p.setString(1,m.version);p.setString(2,m.name);p.setString(3,m.checksum);p.setString(4,status);
            p.setTimestamp(5,status.equals("SUCCESS")?Timestamp.from(Instant.now()):null);p.executeUpdate();
        }
    }
    private static void finish(Connection c,String version,String status) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("UPDATE schema_migration SET status=?,finished_at=CURRENT_TIMESTAMP(6) WHERE `迁移版本`=?")) {
            p.setString(1,status);p.setString(2,version);p.executeUpdate();
        }
    }
    private static void run(Connection c,String sql) throws SQLException { try(Statement st=c.createStatement()) {st.execute(sql);} }
}

