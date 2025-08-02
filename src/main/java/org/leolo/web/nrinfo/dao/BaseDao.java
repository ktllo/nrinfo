package org.leolo.web.nrinfo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BaseDao {

    protected boolean parseBitAsBoolean(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return value != 0;
    }
}
