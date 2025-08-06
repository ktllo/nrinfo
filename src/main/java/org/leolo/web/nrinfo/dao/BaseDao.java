package org.leolo.web.nrinfo.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class BaseDao {

    public static final int BATCH_SIZE = 1000;

    protected boolean parseBitAsBoolean(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return value != 0;
    }

    protected void setString(PreparedStatement ps, int pos, String data) throws SQLException {
        if(data == null || data.isBlank()) {
            ps.setNull(pos, Types.VARCHAR);
        } else {
            ps.setString(pos, data);
        }
    }
}
