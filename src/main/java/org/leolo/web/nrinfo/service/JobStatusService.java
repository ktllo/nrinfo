package org.leolo.web.nrinfo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.UUID;

@Component
public class JobStatusService {
    private Logger logger = LoggerFactory.getLogger(JobStatusService.class);

    @Autowired
    DataSource dataSource;

    public Date getLastRunTime(String jobId){
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "select last_run from job_last_run_info where job_id=?");
        ){
            pstmt.setString(1, jobId);
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    Timestamp ts = rs.getTimestamp(1);
                    if(!rs.wasNull()){
                        return new Date(ts.getTime());
                    }
                    logger.debug("Last run time for job {}  is null", jobId);
                } else {
                    logger.info("Unable to find last run time for job {}", jobId);
                }
            }
            return null;
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
            return null;
        }
    }

    public void insertJob(UUID jobId, String jobType) {
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(
                        "INSERT INTO job_status (job_id, job_type, created_date, updated_date) VALUES (?,?,now(), now())")
        ) {
            preparedStatement.setString(1, jobId.toString());
            preparedStatement.setString(2, jobType);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        }
    }

    public void markJobFailed(UUID jobId, String reason){
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE job_status SET status=?,updated_date=now(),detail_message=? WHERE job_id=?"
                )
        ) {
            pstmt.setString(1, "F");
            pstmt.setString(2, reason);
            pstmt.setString(3, jobId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        }
    }
}
