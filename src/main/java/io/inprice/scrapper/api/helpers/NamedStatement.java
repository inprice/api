package io.inprice.scrapper.api.helpers;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedStatement implements AutoCloseable {

    private PreparedStatement prepStmt;
    private List<String> fields = new ArrayList<>();

    public NamedStatement(Connection con, String query) throws SQLException {
        this(con, query, false);
    }

    public NamedStatement(Connection conn, String query, boolean hasGeneratedKeys) throws SQLException {
        //Pattern findParametersPattern = Pattern.compile("(?<!')(:[\\w]*)(?!')");
        final Pattern pattern = Pattern.compile("(?!\\B'[^']*)(:\\w+)(?![^']*'\\B)");
        Matcher matcher = pattern.matcher(query);
        while (matcher.find()) {
            fields.add(matcher.group().substring(1));
        }

        if (hasGeneratedKeys) {
            prepStmt = conn.prepareStatement(query.replaceAll(pattern.pattern(), "?"), Statement.RETURN_GENERATED_KEYS);
        } else {
            prepStmt = conn.prepareStatement(query.replaceAll(pattern.pattern(), "?"));
        }
    }

    public PreparedStatement getPreparedStatement() {
        return prepStmt;
    }

    public ResultSet executeQuery() throws SQLException {
        return prepStmt.executeQuery();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return prepStmt.getGeneratedKeys();
    }

    public int executeUpdate() throws SQLException {
        return prepStmt.executeUpdate();
    }

    public void close() throws SQLException {
        prepStmt.close();
    }

    public void setInt(String name, int value) throws SQLException {
        prepStmt.setInt(getIndex(name), value);
    }

    public void setLong(String name, long value) throws SQLException {
        prepStmt.setLong(getIndex(name), value);
    }

    public void setString(String name, String value) throws SQLException {
        prepStmt.setString(getIndex(name), value);
    }

    public void setBoolean(String name, boolean value) throws SQLException {
        prepStmt.setBoolean(getIndex(name), value);
    }

    private int getIndex(String name) {
        return fields.indexOf(name)+1;
    }
}