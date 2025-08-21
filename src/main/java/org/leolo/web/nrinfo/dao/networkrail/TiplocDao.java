package org.leolo.web.nrinfo.dao.networkrail;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.networkrail.schedule.Tiploc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TiplocDao extends BaseDao {

    @Autowired
    private DataSource dataSource;

    public void insertOrUpdateTiploc(Tiploc tiploc) {
        try (Connection connection = dataSource.getConnection()) {
            Tiploc oldTiploc = getTiplocByTiploc(tiploc.getTiplocCode());
            if (oldTiploc == null) {
                //We need to insert
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tiploc " +
                                "(tiploc_code, nalco_code, stanox_code, crs_code, description, tps_description) " +
                                "VALUES " +
                                "(?,?,?,?,?,?)"
                )) {
                    setString(statement, 1, tiploc.getTiplocCode());
                    setString(statement, 2, tiploc.getNalco());
                    setString(statement, 3, tiploc.getStanox());
                    setString(statement, 4, tiploc.getCrsCode());
                    setString(statement, 5, tiploc.getDescription());
                    setString(statement, 6, tiploc.getTpsDescription());
                    statement.execute();
                }
            } else if (!oldTiploc.equals(tiploc)) {
                //We need to update
                updateTiploc(tiploc);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTiploc(Tiploc tiploc) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tiploc " +
                            "SET nalco_code=?, stanox_code=?, crs_code=?, description=?, tps_description=? " +
                            "WHERE tiploc_code=?")
        ) {
            setString(statement, 1, tiploc.getNalco());
            setString(statement, 2, tiploc.getStanox());
            setString(statement, 3, tiploc.getCrsCode());
            setString(statement, 4, tiploc.getDescription());
            setString(statement, 5, tiploc.getTpsDescription());
            setString(statement, 6, tiploc.getTiplocCode());
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Tiploc getTiplocByTiploc(String tiploc) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * from tiploc where tiploc_code = ?"
                )
        ){
            ps.setString(1, tiploc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Tiploc result = new Tiploc();
                    result.setTiplocCode(rs.getString("tiploc_code"));
                    result.setNalco(rs.getString("nalco_code"));
                    result.setStanox(rs.getString("stanox_code"));
                    result.setCrsCode(rs.getString("crs_code"));
                    result.setDescription(rs.getString("description"));
                    result.setTpsDescription(rs.getString("tps_description"));
                    return result;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void deleteTiploc(String tiplocCode) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM tiploc WHERE tiploc_code = ?"
                )
        ) {
            ps.setString(1, tiplocCode);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteTiploc(Tiploc tiploc) {
        deleteTiploc(tiploc.getTiplocCode());
    }
}
