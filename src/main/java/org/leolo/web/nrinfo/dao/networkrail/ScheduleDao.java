package org.leolo.web.nrinfo.dao.networkrail;

import lombok.SneakyThrows;
import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.networkrail.schedule.Schedule;
import org.leolo.web.nrinfo.model.networkrail.schedule.ScheduleEntry;
import org.leolo.web.nrinfo.model.networkrail.schedule.ScheduleIndicator;
import org.leolo.web.nrinfo.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;

@Component
public class ScheduleDao extends BaseDao {

    @Autowired private DataSource dataSource;

    private Logger log = LoggerFactory.getLogger(ScheduleDao.class);
    @Autowired
    private EndpointMediaTypes endpointMediaTypes;

    public void insertSchedule(Collection<Schedule> schedules) {
        int inserted = 0;
        int totalEntry = 0;
        HashSet<PrimaryKey> insertedKeys = new HashSet<>();
        try(
                Connection connection = dataSource.getConnection();
                PreparedStatement psChk = connection.prepareStatement(
                        "SELECT 1 FROM schedule WHERE train_uid = ? and start_date = ? and end_date = ? and stp_indicator = ?"
                );
                PreparedStatement psInsBase = connection.prepareStatement(
                        "INSERT INTO schedule (" +
                                "train_uid, start_date, end_date, stp_indicator, days_runs, " +
                                "atoc_code, from_location, to_location, depart_time, arrive_time, " +
                                "record_uuid" +
                                ") VALUES (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?" +
                                ")"
                );
                PreparedStatement psInsDetail = connection.prepareStatement(
                        "INSERT INTO schedule_details (" +
                                "record_uuid, runs_on_bank_holiday, train_status, subject_to_performance_monitor, train_category, " +
                                "signalling_head_code, reservation_systeam_head_code, train_service_code, portion_id, power_type, " +
                                "timing_load, planned_speed, operating_characteristics, has_first_class, sleeper, " +
                                "reservation, catering, location_count" +
                                ") VALUES (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?" +
                                ")"
                );
                PreparedStatement psInsEnt = connection.prepareStatement(
                        "INSERT INTO schedule_entry (" +
                                "record_uuid, entry_sequence, location, location_instance, wtt_arrival, " +
                                "wtt_pass, wtt_departure, gbtt_arrival, gbtt_departure, platform, " +
                                "line, path, engineering_allowance, pathing_allowance, performance_allowance" +
                                ") VALUES (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?,?,?" +
                                ")"
                )
        ) {
            connection.setAutoCommit(false);
            for (Schedule schedule : schedules) {
                psChk.setString(1, schedule.getTrainUID());
                setDate(psChk, 2, schedule.getStartDate());
                setDate(psChk, 3, schedule.getEndDate());
                psChk.setString(4, schedule.getStpIndicator().getCode());
                try (ResultSet rs = psChk.executeQuery()) {
                    if (rs.next()) {
                        log.warn("Schedule {}-{}-{}-{} already exists. Skipping", schedule.getTrainUID(), schedule.getStartDate(), schedule.getEndDate(), schedule.getStpIndicator());
                        continue;
                    }
                }
                //Assign UUID
                schedule.setRecordUUID(CommonUtil.generateUUID());
                psInsBase.setString(1, schedule.getTrainUID());
                setDate(psInsBase, 2, schedule.getStartDate());
                setDate(psInsBase, 3, schedule.getEndDate());
                psInsBase.setString(4, schedule.getStpIndicator().getCode());
                psInsBase.setString(5, schedule.getDaysRuns());
                psInsBase.setString(6, schedule.getAtocCode());
                if (schedule.getStpIndicator() == ScheduleIndicator.CANCELLATION || schedule.getScheduleLocations().isEmpty()) {
                    //No basic schedule information
                    psInsBase.setNull(7, Types.VARCHAR);
                    psInsBase.setNull(8, Types.VARCHAR);
                    psInsBase.setNull(9, Types.TIME);
                    psInsBase.setNull(10, Types.TIME);
                } else {
                    ScheduleEntry first = schedule.getScheduleLocations().get(0);
                    psInsBase.setString(7, first.getLocation());
                    psInsBase.setTime(9, first.getGbttDeparture()==null?first.getWttDeparture():first.getGbttDeparture());
                    ScheduleEntry last = schedule.getScheduleLocations().get(schedule.getScheduleLocations().size() - 1);
                    psInsBase.setString(8, last.getLocation());
                    psInsBase.setTime(10, last.getGbttArrival()==null?last.getWttArrival():last.getGbttArrival());
                }
                psInsBase.setBytes(11, CommonUtil.uuidToBytes(schedule.getRecordUUID()));
                psInsBase.addBatch();
                if (schedule.getStpIndicator() == ScheduleIndicator.CANCELLATION) {
                    //There are no details for cancellations
                    continue;
                }
                psInsDetail.setBytes(1, CommonUtil.uuidToBytes(schedule.getRecordUUID()));
                psInsDetail.setString(2, schedule.getRunsOnBankHolidays()==null?"N":schedule.getRunsOnBankHolidays());
                psInsDetail.setString(3, schedule.getTrainStatus()==null?"U":schedule.getTrainStatus());
                psInsDetail.setString(4, schedule.isSubjectToPerformanceMonitor()?"Y":"N");
                psInsDetail.setString(5, schedule.getTrainCategory());
                psInsDetail.setString(6, schedule.getSignallingHeadCode());
                psInsDetail.setString(7, schedule.getReservationSystemHeadCode());
                psInsDetail.setString(8, schedule.getTrainServiceCode());
                if (!"??".equals(schedule.getPortionId())) {
                    psInsDetail.setString(9, schedule.getPortionId());
                } else {
                    psInsDetail.setNull(9, Types.VARCHAR);
                }
                psInsDetail.setString(10, schedule.getPowerType());
                psInsDetail.setString(11, schedule.getTimingLoad());
                psInsDetail.setInt(12, schedule.getPlannedSpeed());
                psInsDetail.setString(13, schedule.getOperatingCharacteristics());
                psInsDetail.setString(14, schedule.isHasFirstClass()?"Y":"N");
                psInsDetail.setString(15, schedule.getSleeper()==null?"N":schedule.getSleeper());
                psInsDetail.setString(16, schedule.getReservations()==null?"N":schedule.getReservations());
                psInsDetail.setString(17, schedule.getCatering());
                psInsDetail.setInt(18, schedule.getScheduleLocations().size());
                psInsDetail.addBatch();
                psInsEnt.setBytes(1, CommonUtil.uuidToBytes(schedule.getRecordUUID()));
                for(int i=0;i<schedule.getScheduleLocations().size();i++) {
                    psInsEnt.setInt(2, i+1);
                    ScheduleEntry entry = schedule.getScheduleLocations().get(i);
                    psInsEnt.setString(3, entry.getLocation());
                    psInsEnt.setInt(4, entry.getLocationInstance());
                    psInsEnt.setTime(5, entry.getWttArrival());
                    psInsEnt.setTime(6, entry.getWttPass());
                    psInsEnt.setTime(7, entry.getWttDeparture());
                    psInsEnt.setTime(8, entry.getGbttArrival());
                    psInsEnt.setTime(9, entry.getGbttDeparture());
                    psInsEnt.setString(10, entry.getPlatform());
                    psInsEnt.setString(11, entry.getLine());
                    psInsEnt.setString(12, entry.getPath());
                    psInsEnt.setTime(13, entry.getEngineeringAllowance());
                    psInsEnt.setTime(14, entry.getPathingAllowance());
                    psInsEnt.setTime(15, entry.getPerformanceAllowance());
                    psInsEnt.addBatch();
                }
            }
            psInsBase.executeBatch();
            psInsDetail.executeBatch();
            psInsEnt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            log.info("Unable to insert schedules - {}", e.getMessage(), e);
        }

    }

    public UUID getScheduleUUID(String uid, Date startDate, Date endDate, ScheduleIndicator indicator) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement psBase = connection.prepareStatement(
                        "SELECT " +
                                "record_uuid " +
                                "FROM schedule WHERE train_uid=? AND start_date=? AND end_date=? AND stp_indicator=?"
                );
        ) {
            psBase.setString(1, uid);
            setDate(psBase, 2, startDate);
            setDate(psBase, 3, endDate);
            psBase.setString(4, indicator.getCode());
            try (ResultSet rs = psBase.executeQuery()) {
                if (rs.next()) {
                    return CommonUtil.bytesToUUID(rs.getBytes(1));
                }
            }
        } catch (SQLException e) {
            log.info("Unable to get schedule uuid - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }
    public Schedule getSchedule(String uid, Date startDate, Date endDate, ScheduleIndicator indicator) {
        Schedule schedule = new Schedule();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement psBase = connection.prepareStatement(
                        "SELECT " +
                                "train_uid, start_date, end_date, stp_indicator, days_runs, atoc_code, record_uuid " +
                                "FROM schedule WHERE train_uid=? AND start_date=? AND end_date=? AND stp_indicator=?"
                );
                PreparedStatement psDetail = connection.prepareStatement(
                        "SELECT " +
                                "runs_on_bank_holiday, train_status, subject_to_performance_monitor, train_category, signalling_head_code, " +
                                "reservation_systeam_head_code, train_service_code, portion_id, power_type, timing_load, " +
                                "planned_speed, operating_characteristics, has_first_class, sleeper, reservation, " +
                                "catering " +
                                "FROM schedule_details where record_uuid = ?"
                );
                PreparedStatement psEntry = connection.prepareStatement(
                        "SELECT " +
                                "location, location_instance, wtt_arrival, wtt_pass, wtt_departure, " +
                                "gbtt_arrival, gbtt_departure, platform, line, path, " +
                                "engineering_allowance, pathing_allowance, performance_allowance " +
                                "FROM schedule_entry WHERE record_uuid = ?"
                )
        ) {
            psBase.setString(1, uid);
            setDate(psBase, 2, startDate);
            setDate(psBase, 3, endDate);
            psBase.setString(4, indicator.getCode());
            byte [] uuid = null;
            try (ResultSet rs = psBase.executeQuery()) {
                if (rs.next()) {
                    //Set the basic information. The location info will be skipped in this part
                    schedule.setTrainUID(rs.getString(1));
                    schedule.setStartDate(rs.getDate(2));
                    schedule.setEndDate(rs.getDate(3));
                    schedule.setStpIndicator(ScheduleIndicator.fromCode(rs.getString(4)));
                    schedule.setAtocCode(rs.getString(5));
                    uuid = rs.getBytes(6);
                    schedule.setRecordUUID(CommonUtil.bytesToUUID(uuid));
                } else {
                    // No Such schedule
                    return null;
                }
            }
            psDetail.setBytes(1, uuid);
            try (ResultSet rs = psDetail.executeQuery()) {
                if (rs.next()) {
                    schedule.setRunsOnBankHolidays(rs.getString(1));
                    schedule.setTrainStatus(rs.getString(2));
                    schedule.setSubjectToPerformanceMonitor("Y".equals(rs.getString(3)));
                    schedule.setTrainCategory(rs.getString(4));
                    schedule.setSignallingHeadCode(rs.getString(5));
                    schedule.setReservationSystemHeadCode(rs.getString(6));
                    schedule.setTrainServiceCode(rs.getString(7));
                    schedule.setPortionId(rs.getString(8));
                    schedule.setPowerType(rs.getString(9));
                    schedule.setTimingLoad(rs.getString(10));
                    schedule.setPlannedSpeed(rs.getInt(11));
                    schedule.setOperatingCharacteristics(rs.getString(12));
                    schedule.setHasFirstClass("Y".equals(rs.getString(13)));
                    schedule.setSleeper(rs.getString(14));
                    schedule.setReservations(rs.getString(15));
                    schedule.setCatering(rs.getString(16));
                }
            }
            psEntry.setBytes(1, uuid);
            try (ResultSet rs = psEntry.executeQuery()) {
                while (rs.next()) {
                    ScheduleEntry entry = new ScheduleEntry();
                    entry.setLocation(rs.getString(1));
                    entry.setLocationInstance(rs.getInt(2));
                    entry.setWttArrival(rs.getTime(3));
                    entry.setWttPass(rs.getTime(4));
                    entry.setWttDeparture(rs.getTime(5));
                    entry.setGbttArrival(rs.getTime(6));
                    entry.setGbttDeparture(rs.getTime(7));
                    entry.setPlatform(rs.getString(8));
                    entry.setLine(rs.getString(9));
                    entry.setPath(rs.getString(10));
                    entry.setEngineeringAllowance(rs.getTime(11));
                    entry.setPathingAllowance(rs.getTime(12));
                    entry.setPerformanceAllowance(rs.getTime(13));
                    schedule.getScheduleLocations().add(entry);
                }
            }
        } catch (SQLException e) {
            log.error("Unable to get schedule - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return schedule;
    }

    public void updateSchedules(Collection<Schedule> schedules) {
        try (
                Connection connection = dataSource.getConnection();

                PreparedStatement psInsDetail = connection.prepareStatement(
                        "INSERT INTO schedule_details (" +
                                "record_uuid, runs_on_bank_holiday, train_status, subject_to_performance_monitor, train_category, " +
                                "signalling_head_code, reservation_systeam_head_code, train_service_code, portion_id, power_type, " +
                                "timing_load, planned_speed, operating_characteristics, has_first_class, sleeper, " +
                                "reservation, catering, location_count" +
                                ") VALUES (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?" +
                                ")"
                );
                PreparedStatement psInsEnt = connection.prepareStatement(
                        "INSERT INTO schedule_entry (" +
                                "record_uuid, entry_sequence, location, location_instance, wtt_arrival, " +
                                "wtt_pass, wtt_departure, gbtt_arrival, gbtt_departure, platform, " +
                                "line, path, engineering_allowance, pathing_allowance, performance_allowance" +
                                ") VALUES (" +
                                "?,?,?,?,?," +
                                "?,?,?,?,?," +
                                "?,?,?,?,?" +
                                ")"
                );
                PreparedStatement psMain = connection.prepareStatement(
                        "UPDATE schedule SET " +
                                "days_runs=?, atoc_code=?, from_location=?, to_location=?, depart_time=?, " +
                                "arrive_time = ?, record_uuid = ? " +
                                "WHERE train_uid=? and start_date=? and end_date=? and stp_indicator=?"
                );
                PreparedStatement psEntry = connection.prepareStatement(
                        "DELETE FROM schedule_details WHERE record_uuid = ?"
                );
                PreparedStatement psDetail = connection.prepareStatement(
                        "DELETE FROM schedule_entry WHERE record_uuid = ?"
                );
        ) {
            connection.setAutoCommit(false);
            for (Schedule schedule : schedules) {
                byte [] newUUID = CommonUtil.uuidToBytes(CommonUtil.generateUUID());
                byte [] oldUUID = CommonUtil.uuidToBytes(getScheduleUUID(
                        schedule.getTrainUID(),
                        schedule.getStartDate(),
                        schedule.getEndDate(),
                        schedule.getStpIndicator()
                ));
                if (schedule.getStpIndicator() != ScheduleIndicator.CANCELLATION) {
                    psInsDetail.setBytes(1, CommonUtil.uuidToBytes(schedule.getRecordUUID()));
                    psInsDetail.setString(2, schedule.getRunsOnBankHolidays() == null ? "N" : schedule.getRunsOnBankHolidays());
                    psInsDetail.setString(3, schedule.getTrainStatus() == null ? "U" : schedule.getTrainStatus());
                    psInsDetail.setString(4, schedule.isSubjectToPerformanceMonitor() ? "Y" : "N");
                    psInsDetail.setString(5, schedule.getTrainCategory());
                    psInsDetail.setString(6, schedule.getSignallingHeadCode());
                    psInsDetail.setString(7, schedule.getReservationSystemHeadCode());
                    psInsDetail.setString(8, schedule.getTrainServiceCode());
                    if (!"??".equals(schedule.getPortionId())) {
                        psInsDetail.setString(9, schedule.getPortionId());
                    } else {
                        psInsDetail.setNull(9, Types.VARCHAR);
                    }
                    psInsDetail.setString(10, schedule.getPowerType());
                    psInsDetail.setString(11, schedule.getTimingLoad());
                    psInsDetail.setInt(12, schedule.getPlannedSpeed());
                    psInsDetail.setString(13, schedule.getOperatingCharacteristics());
                    psInsDetail.setString(14, schedule.isHasFirstClass() ? "Y" : "N");
                    psInsDetail.setString(15, schedule.getSleeper() == null ? "N" : schedule.getSleeper());
                    psInsDetail.setString(16, schedule.getReservations() == null ? "N" : schedule.getReservations());
                    psInsDetail.setString(17, schedule.getCatering());
                    psInsDetail.setInt(18, schedule.getScheduleLocations().size());
                    psInsDetail.addBatch();
                    psInsEnt.setBytes(1, CommonUtil.uuidToBytes(schedule.getRecordUUID()));
                    for (int i = 0; i < schedule.getScheduleLocations().size(); i++) {
                        psInsEnt.setInt(2, i + 1);
                        ScheduleEntry entry = schedule.getScheduleLocations().get(i);
                        psInsEnt.setString(3, entry.getLocation());
                        psInsEnt.setInt(4, entry.getLocationInstance());
                        psInsEnt.setTime(5, entry.getWttArrival());
                        psInsEnt.setTime(6, entry.getWttPass());
                        psInsEnt.setTime(7, entry.getWttDeparture());
                        psInsEnt.setTime(8, entry.getGbttArrival());
                        psInsEnt.setTime(9, entry.getGbttDeparture());
                        psInsEnt.setString(10, entry.getPlatform());
                        psInsEnt.setString(11, entry.getLine());
                        psInsEnt.setString(12, entry.getPath());
                        psInsEnt.setTime(13, entry.getEngineeringAllowance());
                        psInsEnt.setTime(14, entry.getPathingAllowance());
                        psInsEnt.setTime(15, entry.getPerformanceAllowance());
                        psInsEnt.addBatch();
                    }
                }
                psMain.setString(1, schedule.getDaysRuns());
                psMain.setString(2, schedule.getAtocCode());
                if (schedule.getStpIndicator() == ScheduleIndicator.CANCELLATION || schedule.getScheduleLocations().isEmpty()) {
                    //No basic schedule information
                    psMain.setNull(3, Types.VARCHAR);
                    psMain.setNull(4, Types.VARCHAR);
                    psMain.setNull(5, Types.TIME);
                    psMain.setNull(6, Types.TIME);
                } else {
                    ScheduleEntry first = schedule.getScheduleLocations().get(0);
                    psMain.setString(3, first.getLocation());
                    psMain.setTime(4, first.getGbttDeparture()==null?first.getWttDeparture():first.getGbttDeparture());
                    ScheduleEntry last = schedule.getScheduleLocations().get(schedule.getScheduleLocations().size() - 1);
                    psMain.setString(5, last.getLocation());
                    psMain.setTime(6, last.getGbttArrival()==null?last.getWttArrival():last.getGbttArrival());
                }
                psMain.setBytes(7, newUUID);
                psMain.setString(8, schedule.getTrainUID());
                setDate(psMain, 9, schedule.getStartDate());
                setDate(psMain, 10, schedule.getEndDate());
                psMain.setString(11, schedule.getStpIndicator().getCode());
                psMain.addBatch();
                psEntry.setBytes(1, oldUUID);
                psDetail.setBytes(1, oldUUID);
                psDetail.addBatch();
                psEntry.addBatch();
            }
            psInsDetail.executeBatch();
            psInsEnt.executeBatch();
            psMain.executeBatch();
            psEntry.executeBatch();
            psDetail.executeBatch();
            connection.commit();
        } catch (SQLException sqle) {
            log.info("Unable to update schedule - {}", sqle.getMessage(), sqle);
            throw new RuntimeException(sqle);
        }
    }

    public void deleteSchedule(String uid, Date startDate, Date endDate, ScheduleIndicator indicator) {
        deleteSchedule(CommonUtil.uuidToBytes(getScheduleUUID(uid, startDate, endDate, indicator)));
    }

    public void deleteSchedule(Schedule schedule) {
        byte [] uuid = CommonUtil.uuidToBytes(getScheduleUUID(schedule.getTrainUID(), schedule.getStartDate(), schedule.getEndDate(), schedule.getStpIndicator()));
        deleteSchedule(uuid);
    }

    private void deleteSchedule(byte [] uuid) {
        if (uuid == null) {
            try (
                    Connection conn = dataSource.getConnection();
                    PreparedStatement psEntry = conn.prepareStatement(
                            "DELETE FROM schedule_details WHERE record_uuid = ?"
                    );
                    PreparedStatement psDetail = conn.prepareStatement(
                            "DELETE FROM schedule_entry WHERE record_uuid = ?"
                    );
                    PreparedStatement psBase = conn.prepareStatement(
                            "DELETE FROM schedule WHERE record_uuid = ?"
                    )
            ) {
                conn.setAutoCommit(false);
                psBase.setBytes(1, uuid);
                psDetail.setBytes(1, uuid);
                psEntry.setBytes(1, uuid);
                psEntry.executeUpdate();
                psDetail.executeUpdate();
                psBase.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                log.error("Unable to delete schedule - {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteSchedules(Collection<Schedule> schedules) {
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement psEntry = conn.prepareStatement(
                        "DELETE FROM schedule_details WHERE record_uuid = ?"
                );
                PreparedStatement psDetail = conn.prepareStatement(
                        "DELETE FROM schedule_entry WHERE record_uuid = ?"
                );
                PreparedStatement psBase = conn.prepareStatement(
                        "DELETE FROM schedule WHERE record_uuid = ?"
                )
        ) {
            conn.setAutoCommit(false);
            for (Schedule schedule : schedules) {
                byte [] uuid = CommonUtil.uuidToBytes(getScheduleUUID(schedule.getTrainUID(), schedule.getStartDate(), schedule.getEndDate(), schedule.getStpIndicator()));
                if (uuid != null) {
                    psEntry.setBytes(1, uuid);
                    psDetail.setBytes(1, uuid);
                    psBase.setBytes(1, uuid);
                    psEntry.addBatch();
                    psDetail.addBatch();
                    psBase.addBatch();
                }
            }
            psEntry.executeBatch();
            psDetail.executeBatch();
            psBase.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            log.error("Unable to delete schedule - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static class PrimaryKey {
        String trainUID;
        Date startDate;
        Date endDate;
        ScheduleIndicator stpIndicator;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PrimaryKey that)) return false;
            return Objects.equals(trainUID, that.trainUID) && Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate) && stpIndicator == that.stpIndicator;
        }

        @Override
        public int hashCode() {
            return Objects.hash(trainUID, startDate, endDate, stpIndicator);
        }
    }
}
