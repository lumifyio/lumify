package io.lumify.palantir.sqlrunner;

import java.sql.*;

public class SqlRunner {
    private final String connectionString;
    private final String username;
    private final String password;
    private final String tableNamespace;
    private Connection connection;

    public SqlRunner(String connectionString, String username, String password, String tableNamespace) {
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
        this.tableNamespace = tableNamespace;
    }

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("oracle.jdbc.driver.OracleDriver");

        connection = DriverManager.getConnection(connectionString, username, password);
        if (connection == null) {
            throw new RuntimeException("Could not create connection: " + connectionString);
        }
    }

    public void close() throws SQLException {
        connection.close();
    }

    public <T> Iterable<T> select(String sql, Class<T> clazz) {
        try {
            sql = sql.replaceAll("\\{namespace\\}", tableNamespace);
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            return new SqlRunnerQueryIterable<T>(rs, clazz);
        } catch (SQLException ex) {
            throw new RuntimeException("Could no run sql: " + sql, ex);
        }
    }
}
