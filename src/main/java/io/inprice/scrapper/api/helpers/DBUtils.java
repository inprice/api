package io.inprice.scrapper.api.helpers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.common.helpers.ModelMapper;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBUtils {

    private static final Logger log = new Logger(DBUtils.class);

    private final Config config = Beans.getSingleton(Config.class);
    private final HikariDataSource ds;

    private DBUtils() {
        HikariConfig hConf = new HikariConfig();

        hConf.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf8", config.getDB_Host(), config.getDB_Port(), config.getDB_Database()));
        hConf.setUsername(config.getDB_Username());
        hConf.setPassword(config.getDB_Password());
        hConf.addDataSourceProperty("cachePrepStmts", "true");
        hConf.addDataSourceProperty("prepStmtCacheSize", "250");
        hConf.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hConf.addDataSourceProperty("useServerPrepStmts", "true");
        hConf.addDataSourceProperty("useLocalSessionState", "true");
        hConf.addDataSourceProperty("rewriteBatchedStatements", "true");
        hConf.addDataSourceProperty("cacheResultSetMetadata", "true");
        hConf.addDataSourceProperty("cacheServerConfiguration", "true");
        hConf.addDataSourceProperty("elideSetAutoCommits", "true");
        hConf.addDataSourceProperty("maintainTimeStats", "false");

        ds = new HikariDataSource(hConf);
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public Connection getTransactionalConnection() throws SQLException {
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        return con;
    }

    public void commit(Connection con) {
        try {
            con.commit();
        } catch (SQLException ex) {
            //
        }
    }

    public void rollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ex) {
            //
        }
    }

    public void close(Connection con) {
        try {
            con.close();
        } catch (SQLException ex) {
            //
        }
    }

    private void close(Connection con, Statement pst) {
        try {
            pst.close();
            con.close();
        } catch (SQLException ex) {
            //
        }
    }

    public <M extends Model> M findSingle(String query, ModelMapper<M> mapper) {
        List<M> result = findMultiple(query, mapper);

        if (result != null && result.size() > 0)
            return result.get(0);
        else
            return null;
    }

    public <M extends Model> List<M> findMultiple(String query, ModelMapper<M> mapper) {
        List<M> result = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                result.add(mapper.map(rs));
            }
        } catch (Exception e) {
            log.error("Failed to fetch models", e);
        }

        return result;
    }

    /**
     * For single executions with a continual transaction
     *
     */
    public boolean executeQuery(Connection con, String query, String errorMessage) {
        if (con == null) {
            return executeQuery(query, errorMessage);
        }

        try (PreparedStatement pst = con.prepareStatement(query)) {
            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error(errorMessage, e);
        }
        return false;
    }

    /**
     * For single executions without any continual transaction
     *
     */
    public boolean executeQuery(String query, String errorMessage) {
        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            log.error(errorMessage, e);
        }
        return false;
    }

    public boolean executeBatchQueries(Connection con, String[] queries, String errorMessage) {
        if (con == null) {
            return executeBatchQueries(queries, errorMessage);
        }

        boolean result = false;

        try (Statement sta = con.createStatement()) {
            for (String query: queries) {
                sta.addBatch(query);
            }

            int[] affected = sta.executeBatch();

            result = true;
            for (int aff: affected) {
                if (aff < 1) return false;
            }
        } catch (SQLException e) {
            log.error(errorMessage, e);
        }
        return result;
    }

    /**
     * For batch executions without any continual transaction
     *
     */
    public boolean executeBatchQueries(String[] queries, String errorMessage) {
        boolean result = false;

        Connection con = null;
        Statement sta = null;
        try {
            con = getTransactionalConnection();
            sta = con.createStatement();

            for (String query: queries) {
                sta.addBatch(query);
            }

            int[] affected = sta.executeBatch();

            result = true;
            for (int aff: affected) {
                if (aff < 1) {
                    rollback(con);
                    result = false;
                    break;
                }
            }
            commit(con);
        } catch (SQLException e) {
            rollback(con);
            log.error(errorMessage, e);
        } finally {
            close(con, sta);
        }
        return result;
    }

    public void shutdown() {
        ds.close();
    }

}
