package org.leolo.web.nrinfo.dao.networkrail;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.networkrail.Smart;
import org.leolo.web.nrinfo.model.networkrail.SmartEventType;
import org.leolo.web.nrinfo.model.networkrail.SmartStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Collection;

@Component
public class SmartDao extends BaseDao {


    private Logger logger = LoggerFactory.getLogger(SmartDao.class);

    public static final int BATCH_SIZE = 1000;

    @Autowired
    private DataSource dataSource;

    public void truncateTable() {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()
        ){
            statement.executeUpdate("TRUNCATE TABLE smart");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addAll(Collection<Smart> smarts) {
        int count = 0;
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO smart (" +
                                "train_describer_area, from_berth, to_berth, from_line, to_line, " +
                                "berth_offset, platform, event, route, stanox_code, " +
                                "station_name, step_type, comment" +
                                ") VALUE (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?)"
                )
        ){
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            for (Smart smart: smarts) {
                ps.setString(1, smart.getTrainDescriberArea());
                ps.setString(2, smart.getFromBerth());
                ps.setString(3, smart.getToBerth());
                ps.setString(4, smart.getFromLine());
                ps.setString(5, smart.getToLine());
                ps.setInt(6, smart.getBerthOffset());
                ps.setString(7, smart.getPlatform());
                if (smart.getEventType() != null) {
                    ps.setString(8, smart.getEventType().getCode());
                } else {
                    ps.setNull(8, Types.VARCHAR);
                }
                ps.setString(9, smart.getRoute());
                ps.setString(10, smart.getStanoxCode());
                ps.setString(11, smart.getStationName());
                if (smart.getStepType() != null) {
                    ps.setString(12, smart.getStepType().getCode());
                } else {
                    ps.setNull(12, Types.VARCHAR);
                }
                ps.setString(13, smart.getComment());
                ps.executeUpdate();
                if (++count % BATCH_SIZE == 0) {
                    connection.commit();
                    logger.info("Commit a SMART batch. Total count = {}", count);
                }
            }
            connection.commit();
            logger.info("Commit a SMART batch. Total count = {}", count);
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addOrReplaceAll(Collection<Smart> smarts) {
        int count = 0;
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (
                    PreparedStatement psIns = connection.prepareStatement(
                            "INSERT INTO smart (" +
                                    "train_describer_area, from_berth, to_berth, from_line, to_line, " +
                                    "berth_offset, platform, event, route, stanox_code, " +
                                    "station_name, step_type, comment" +
                                    ") VALUE (" +
                                    "?,?,?,?,?," +
                                    "?,?,?,?,?," +
                                    "?,?,?)"
                    );
                    PreparedStatement psUpd = connection.prepareStatement(
                            "UPDATE  smart SET " +
                                    "from_line = ?, to_line=?, berth_offset=?, platform=?, event=?," +
                                    "route=?, stanox_code=?, station_name=?, step_type=?, comment=?" +
                                    "WHERE train_describer_area=? and from_berth=? and to_berth=?"
                    )
            ){
                for(Smart smart: smarts) {
                    Smart dbSmart = getSmart(smart.getTrainDescriberArea(), smart.getFromBerth(), smart.getToBerth());
                    if(dbSmart != null) {
                        if (!dbSmart.equals(smart)) {
                            logger.info("Updating SMART({}.{},{})", smart.getTrainDescriberArea(), smart.getFromBerth(), smart.getToBerth());
                            setString(psUpd, 1, smart.getFromLine());
                            setString(psUpd, 2, smart.getToLine());
                            setInt(psUpd, 3, smart.getBerthOffset());
                            setString(psUpd, 4, smart.getPlatform());
                            setString(psUpd, 5, smart.getEventType().getCode());
                            setString(psUpd, 6, smart.getRoute());
                            setString(psUpd, 7, smart.getStanoxCode());
                            setString(psUpd, 8, smart.getStationName());
                            setString(psUpd, 9, smart.getStepType().getCode());
                            setString(psUpd, 10, smart.getComment());
                            setString(psUpd, 11, smart.getTrainDescriberArea());
                            setString(psUpd, 12, smart.getFromBerth());
                            setString(psUpd, 13, smart.getToBerth());
                            psUpd.executeUpdate();
                            if (++count % BATCH_SIZE == 0) {
                                logger.info("Commit a SMART batch. Total count = {}", count);
                                connection.commit();
                            }
                        }
                    } else {
                        setString(psIns, 1, smart.getTrainDescriberArea());
                        setString(psIns, 2, smart.getFromBerth());
                        setString(psIns, 3, smart.getToBerth());
                        setString(psIns, 4, smart.getFromLine());
                        setString(psIns, 5, smart.getToLine());
                        setInt(psIns, 6, smart.getBerthOffset());
                        setString(psIns, 7, smart.getPlatform());
                        setString(psIns, 8, smart.getEventType().getCode());
                        setString(psIns, 9, smart.getRoute());
                        setString(psIns, 10, smart.getStanoxCode());
                        setString(psIns, 11, smart.getStationName());
                        setString(psIns, 12, smart.getStepType().getCode());
                        setString(psIns, 13, smart.getComment());
                        psIns.executeUpdate();
                        if (++count % BATCH_SIZE == 0) {
                            logger.info("Commit a SMART batch. Total count = {}", count);
                            connection.commit();
                        }
                    }
                }
            }
            connection.commit();
            logger.info("Commit a SMART batch. Total count = {}", count);
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Smart getSmart(String trainDescriberArea, String fromBerth, String toBerth) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * from smart WHERE train_describer_area=? and from_berth=? and to_berth=?"
                )
        ) {
            ps.setString(1, trainDescriberArea);
            ps.setString(2, fromBerth);
            ps.setString(3, toBerth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Smart smart = new Smart();
                    smart.setTrainDescriberArea(trainDescriberArea);
                    smart.setFromBerth(fromBerth);
                    smart.setToBerth(toBerth);
                    smart.setFromLine(rs.getString("from_line"));
                    smart.setToLine(rs.getString("to_line"));
                    smart.setBerthOffset(rs.getInt("berth_offset"));
                    smart.setPlatform(rs.getString("platform"));
                    smart.setEventType(SmartEventType.fromCode(rs.getString("event")));
                    smart.setRoute(rs.getString("route"));
                    smart.setStanoxCode(rs.getString("stanox_code"));
                    smart.setStationName(rs.getString("station_name"));
                    smart.setStepType(SmartStepType.fromCode(rs.getString("step_type")));
                    smart.setComment(rs.getString("comment"));
                    return smart;
                }
            }
        }
        return null;
    }

}
