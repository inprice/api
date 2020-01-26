package io.inprice.scrapper.api.helpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.common.helpers.ModelMapper;
import io.inprice.scrapper.common.models.Model;

public class DBUtils {

    private static final Logger log = LoggerFactory.getLogger(DBUtils.class);

    private HikariDataSource ds;

    private DBUtils() {
        wakeup();
    }

    public void reset() {
        if (Props.isRunningForTests()) {
            shutdown();
            wakeup();
        }
    }

    private void wakeup() {
        HikariConfig hConf = new HikariConfig();

        final String connectionString =
                String.format("jdbc:%s:%s:%d/%s%s", Props.getDB_Driver(), Props.getDB_Host(),
                        Props.getDB_Port(), Props.getDB_Database(), Props.getDB_Additions());

        log.info("Connection string: " + connectionString);

        hConf.setJdbcUrl(connectionString);
        hConf.setUsername(Props.getDB_Username());
        hConf.setPassword(Props.getDB_Password());

        if (Props.isRunningForTests())
            hConf.setConnectionTimeout(3 * 1000); //three seconds
        else
            hConf.setConnectionTimeout(10 * 1000); //ten seconds

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
            if (pst != null) pst.close();
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
    	try (Connection con = getConnection()) {
    		result = findMultiple(con, query, mapper);
        } catch (Exception e) {
            log.error("Failed to fetch models", e);
    	}

        return result;
    }
    
    public <M extends Model> List<M> findMultiple(Connection con, String query, ModelMapper<M> mapper) {
        List<M> result = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(query);
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

            if (Props.isShowingSQLQueries()) {
                log.info(" Q-> " + query);
            }

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
        return executeBatchQueries(queries, errorMessage, 0);
    }

    public boolean executeBatchQueries(String[] queries, String errorMessage, int expectedSuccessfulStatementCount) {
        boolean result = false;

        Connection con = null;
        Statement sta = null;
        try {
            con = getTransactionalConnection();
            sta = con.createStatement();

            for (String query: queries) {
                sta.addBatch(query);
                if (Props.isShowingSQLQueries()) {
                    log.info(" Q-> " + query);
                }
            }

            int[] affected = sta.executeBatch();

            result = false;
            int successfulStatementCount = 0;

            for (int aff: affected) {
                if (expectedSuccessfulStatementCount > 0) {
                    if (aff > 0) successfulStatementCount++;
                    if (successfulStatementCount >= expectedSuccessfulStatementCount) {
                        result = true;
                        break;
                    }
                } else {
                    if (aff < 1) {
                        break;
                    }
                }
            }

            if (result) {
                commit(con);
            } else {
                rollback(con);
                if (! Props.isRunningForTests() && ! errorMessage.contains("to delete")) log.error(errorMessage);
            }

        } catch (SQLException e) {
            if (con != null) rollback(con);
            log.error(errorMessage, e);
        } finally {
            if (con != null) close(con, sta);
        }
        return result;
    }

    public void shutdown() {
        ds.close();
    }

}
