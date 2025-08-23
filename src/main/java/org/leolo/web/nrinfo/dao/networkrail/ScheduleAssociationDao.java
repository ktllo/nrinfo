package org.leolo.web.nrinfo.dao.networkrail;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.networkrail.schedule.AssociationType;
import org.leolo.web.nrinfo.model.networkrail.schedule.ScheduleAssociation;
import org.leolo.web.nrinfo.model.networkrail.schedule.ScheduleIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class ScheduleAssociationDao extends BaseDao {

    @Autowired
    private DataSource dataSource;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public ScheduleAssociation getScheduleAssociation(
            String mainUID,
            String assocUID,
            ScheduleIndicator scheduleIndicator,
            java.util.Date startDate,
            java.util.Date endDate
    ) {
        try(
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "select * from schedule_assoc where " +
                                "main_train_uid = ? " +
                                "and assoc_train_uid = ? " +
                                "and stp_indicator = ? " +
                                "and start_date = ? " +
                                "and end_date = ?"
                )
        ) {
            preparedStatement.setString(1, mainUID);
            preparedStatement.setString(2, assocUID);
            preparedStatement.setString(3, scheduleIndicator.getCode());
            setDate(preparedStatement, 4, startDate);
            setDate(preparedStatement, 5, endDate);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return parseScheduleAssociation(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to get schedule association - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    public void insertOrUpdateScheduleAssociation(ScheduleAssociation scheduleAssociation) {
        ScheduleAssociation existingScheduleAssociation = getScheduleAssociation(
                scheduleAssociation.getMainTrainUID(),
                scheduleAssociation.getAssocTrainUID(),
                scheduleAssociation.getStpIndicator(),
                scheduleAssociation.getAssocStartDate(),
                scheduleAssociation.getAssocEndDate()
        );
        if (existingScheduleAssociation == null) {
            insertRecord(scheduleAssociation);
        } else if (!existingScheduleAssociation.equals(scheduleAssociation)) {
            updateScheduleAssociation(existingScheduleAssociation);
        }
    }

    public void insertOrUpdateScheduleAssociation(ArrayList<ScheduleAssociation> scheduleAssociations) {
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO schedule_assoc (" +
                            "uuid, main_train_uid, assoc_train_uid, start_date, end_date, " +
                            "assoc_days, category, assoc_day_diff, location, main_location_suffix, " +
                            "assoc_location_suffix, diagram_type, stp_indicator, last_update" +
                            ") VALUES (" +
                            "?,?,?,?,?," +
                            "?,?,?,?,?," +
                            "?,?,?,NOW()" +
                            ")");
            PreparedStatement psUpd = connection.prepareStatement(
                    "UPDATE schedule_assoc SET " +
                            "assoc_days = ?, category = ?, assoc_day_diff = ?, location=?, main_location_suffix=?, " +
                            "assoc_location_suffix=?, diagram_type = ?, last_update=NOW()" +
                            "WHERE " +
                            "main_train_uid = ? " +
                            "and assoc_train_uid = ? " +
                            "and stp_indicator = ? " +
                            "and start_date = ? " +
                            "and end_date = ?")
        ){
            connection.setAutoCommit(false);
            HashSet<PrimaryKey> primaryKeys = new HashSet<>();
            int ic=0,uc=0;
            for (ScheduleAssociation scheduleAssociation : scheduleAssociations) {
                ScheduleAssociation existingScheduleAssociation = getScheduleAssociation(
                        scheduleAssociation.getMainTrainUID(),
                        scheduleAssociation.getAssocTrainUID(),
                        scheduleAssociation.getStpIndicator(),
                        scheduleAssociation.getAssocStartDate(),
                        scheduleAssociation.getAssocEndDate()
                );
                if (existingScheduleAssociation == null) {
                    PrimaryKey pk = new PrimaryKey(
                            scheduleAssociation.getMainTrainUID(),
                            scheduleAssociation.getAssocTrainUID(),
                            scheduleAssociation.getAssocStartDate(),
                            scheduleAssociation.getAssocEndDate(),
                            scheduleAssociation.getStpIndicator()
                    );
                    if (primaryKeys.contains(pk)) {
                        logger.info("Duplicate entry found. 2nd entry will be skipped");
                        continue;
                    }
                    primaryKeys.add(pk);
                    ins.setString(1, UUID.randomUUID().toString());
                    ins.setString(2, scheduleAssociation.getMainTrainUID());
                    ins.setString(3, scheduleAssociation.getAssocTrainUID());
                    setDate(ins, 4, scheduleAssociation.getAssocStartDate());
                    setDate(ins, 5, scheduleAssociation.getAssocEndDate());
                    ins.setString(6, scheduleAssociation.getAssocDays());
                    if(scheduleAssociation.getAssociationType()!=null) {
                        ins.setString(7, scheduleAssociation.getAssociationType().getCode());
                    } else {
                        ins.setString(7, " ");
                    }
                    ins.setInt(8, scheduleAssociation.getDateIndicator());
                    ins.setString(9, scheduleAssociation.getLocation());
                    ins.setString(10, scheduleAssociation.getBaseLocationSuffix());
                    ins.setString(11, scheduleAssociation.getAssocLocationSuffix());
                    ins.setString(12, scheduleAssociation.getDiagramType());
                    if(scheduleAssociation.getStpIndicator()!=null) {
                        ins.setString(13, scheduleAssociation.getStpIndicator().getCode());
                    } else {
                        ins.setString(13, " ");
                    }
                    ins.addBatch();
                    ic++;
                } else if (!existingScheduleAssociation.equals(scheduleAssociation)) {
                    psUpd.setString(1, scheduleAssociation.getAssocDays());
                    if(scheduleAssociation.getAssociationType()!=null) {
                        psUpd.setString(2, scheduleAssociation.getAssociationType().getCode());
                    } else {
                        psUpd.setString(2, " ");
                    }
                    psUpd.setInt(3, scheduleAssociation.getDateIndicator());
                    psUpd.setString(4, scheduleAssociation.getLocation());
                    psUpd.setString(5, scheduleAssociation.getBaseLocationSuffix());
                    psUpd.setString(6, scheduleAssociation.getAssocLocationSuffix());
                    psUpd.setString(7, scheduleAssociation.getDiagramType());
                    psUpd.setString(8, scheduleAssociation.getMainTrainUID());
                    psUpd.setString(9, scheduleAssociation.getAssocTrainUID());
                    if (scheduleAssociation.getStpIndicator()!=null) {
                        psUpd.setString(10, scheduleAssociation.getStpIndicator().getCode());
                    } else {
                        psUpd.setString(10, " ");
                    }
                    setDate(psUpd, 11, scheduleAssociation.getAssocStartDate());
                    setDate(psUpd, 12, scheduleAssociation.getAssocEndDate());
                    psUpd.addBatch();
                }
            }
            ins.executeBatch();
            psUpd.executeBatch();
            connection.commit();
            logger.info("Inserted {} and updated {} associations", ic, uc);

        } catch (SQLException e) {
            logger.error("Unable to insert schedule association - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void insertRecord(ScheduleAssociation scheduleAssociation) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO schedule_assoc (" +
                                "uuid, main_train_uid, assoc_train_uid, start_date, end_date, " +
                                "assoc_days, category, assoc_day_diff, location, main_location_suffix, " +
                                "assoc_location_suffix, diagram_type, stp_indicator, last_update" +
                                ") VALUES (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?,NOW()" +
                                ")"
                )
        ) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, scheduleAssociation.getMainTrainUID());
            ps.setString(3, scheduleAssociation.getAssocTrainUID());
            setDate(ps, 4, scheduleAssociation.getAssocStartDate());
            setDate(ps, 5, scheduleAssociation.getAssocEndDate());
            ps.setString(6, scheduleAssociation.getAssocDays());
            if(scheduleAssociation.getAssociationType()!=null) {
                ps.setString(7, scheduleAssociation.getAssociationType().getCode());
            } else {
                ps.setString(7, " ");
            }
            ps.setInt(8, scheduleAssociation.getDateIndicator());
            ps.setString(9, scheduleAssociation.getLocation());
            ps.setString(10, scheduleAssociation.getBaseLocationSuffix());
            ps.setString(11, scheduleAssociation.getAssocLocationSuffix());
            ps.setString(12, scheduleAssociation.getDiagramType());
            if(scheduleAssociation.getStpIndicator()!=null) {
                ps.setString(13, scheduleAssociation.getStpIndicator().getCode());
            } else {
                ps.setString(13, " ");
            }
            ps.executeUpdate();
            logger.debug("inserted record into schedule_assoc {}", scheduleAssociation);
        } catch (SQLException e) {
            logger.error("Unable to insert schedule association - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void updateScheduleAssociation(ScheduleAssociation scheduleAssociation) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement psUpd = connection.prepareStatement(
                        "UPDATE schedule_assoc SET " +
                                "assoc_days = ?, category = ?, assoc_day_diff = ?, location=?, main_location_suffix=?, " +
                                "assoc_location_suffix=?, diagram_type = ?, last_update=NOW()" +
                                "WHERE " +
                                "main_train_uid = ? " +
                                "and assoc_train_uid = ? " +
                                "and stp_indicator = ? " +
                                "and start_date = ? " +
                                "and end_date = ?"
                )
        ) {
            psUpd.setString(1, scheduleAssociation.getAssocDays());
            if(scheduleAssociation.getAssociationType()!=null) {
                psUpd.setString(2, scheduleAssociation.getAssociationType().getCode());
            } else {
                psUpd.setString(2, " ");
            }
            psUpd.setInt(3, scheduleAssociation.getDateIndicator());
            psUpd.setString(4, scheduleAssociation.getLocation());
            psUpd.setString(5, scheduleAssociation.getBaseLocationSuffix());
            psUpd.setString(6, scheduleAssociation.getAssocLocationSuffix());
            psUpd.setString(7, scheduleAssociation.getDiagramType());
            psUpd.setString(8, scheduleAssociation.getMainTrainUID());
            psUpd.setString(9, scheduleAssociation.getAssocTrainUID());
            if (scheduleAssociation.getStpIndicator()!=null) {
                psUpd.setString(10, scheduleAssociation.getStpIndicator().getCode());
            } else {
                psUpd.setString(10, " ");
            }
            setDate(psUpd, 11, scheduleAssociation.getAssocStartDate());
            setDate(psUpd, 12, scheduleAssociation.getAssocEndDate());
            psUpd.executeUpdate();
        } catch (SQLException e) {
            logger.error("Unable to update schedule association - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private ScheduleAssociation parseScheduleAssociation(ResultSet rs) throws SQLException {
        ScheduleAssociation sa = new ScheduleAssociation();
        sa.setMainTrainUID(rs.getString("main_train_uid"));
        sa.setAssocTrainUID(rs.getString("assoc_train_uid"));
        sa.setAssocStartDate(rs.getDate("start_date"));
        sa.setAssocEndDate(rs.getDate("end_date"));
        sa.setAssocDays(rs.getString("assoc_days"));
        sa.setAssociationType(AssociationType.fromCode(rs.getString("category")));
        sa.setDateIndicator(rs.getInt("assoc_day_diff"));
        sa.setLocation(rs.getString("location"));
        sa.setBaseLocationSuffix(rs.getString("main_location_suffix"));
        sa.setAssocLocationSuffix(rs.getString("assoc_location_suffix"));
        sa.setDiagramType(rs.getString("diagram_type"));
        sa.setStpIndicator(ScheduleIndicator.fromCode(rs.getString("stp_indicator")));

        return sa;
    }

    private class PrimaryKey {

        String mainTrainUID;
        String assocTrainUID;
        Date assocStartDate;
        Date assocEndDate;
        ScheduleIndicator stpIndicator;

        public PrimaryKey(String mainTrainUID, String assocTrainUID, Date assocStartDate, Date assocEndDate, ScheduleIndicator stpIndicator) {
            this.mainTrainUID = mainTrainUID;
            this.assocTrainUID = assocTrainUID;
            this.assocStartDate = assocStartDate;
            this.assocEndDate = assocEndDate;
            this.stpIndicator = stpIndicator;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PrimaryKey that)) return false;
            return Objects.equals(mainTrainUID, that.mainTrainUID) && Objects.equals(assocTrainUID, that.assocTrainUID) && Objects.equals(assocStartDate, that.assocStartDate) && Objects.equals(assocEndDate, that.assocEndDate) && stpIndicator == that.stpIndicator;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mainTrainUID, assocTrainUID, assocStartDate, assocEndDate, stpIndicator);
        }
    }
}
