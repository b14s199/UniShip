package storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

  static {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:mysql://localhost:3306/uniship");
    config.setUsername("root");
    config.setPassword("root");
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useSSL", "false");
    config.addDataSourceProperty("autoReconnect", "true");
    config.addDataSourceProperty("serverTimezone", "Europe/Rome");
    config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
    config.addDataSourceProperty("leakDetectionThreshold", "6000");
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");

    ds = new HikariDataSource(config);
  }


  public static Connection getConnection() throws SQLException {
    return ds.getConnection();
  }

  static HikariDataSource ds;

}

