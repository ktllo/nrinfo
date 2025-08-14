package org.leolo.web.nrinfo.job.dao;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.job.JobStatus;
import org.leolo.web.nrinfo.job.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class JobDao extends BaseDao {
    @Autowired
    private DataSource dataSource;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void addJob(Job job) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO job_status (job_id, job_type, status, created_date, updated_date, detail_message) VALUES (?,?,?,NOW(),NOW(),NULL)"
                )
        ) {
            preparedStatement.setString(1, job.getJobUUID().toString());
            preparedStatement.setString(2, job.getJobName());
            preparedStatement.setString(3, job.getStatus().getCode());
            preparedStatement.executeUpdate();
            logger.info("Inserted job {}", job.getJobUUID().toString());
        } catch (SQLException e) {
            logger.error("Unable to insert job - {}", e.getMessage(), e);
        }
    }

    public void updateJob(Job job) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "UPDATE job_status SET status = ?, updated_date=NOW() WHERE job_id = ?"
                )
        ) {
            preparedStatement.setString(2, job.getJobUUID().toString());
            preparedStatement.setString(1, job.getStatus().getCode());
            preparedStatement.executeUpdate();
            logger.info("Updated job {}", job.getJobUUID().toString());
        } catch (SQLException e) {
            logger.error("Unable to update job - {}", e.getMessage(), e);
        }
    }

    public Job getJob(String jobUUID) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM job_status WHERE job_id = ?")
        ){
            preparedStatement.setString(1, jobUUID);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    Job job = new Job();
                    job.setJobUUID(UUID.fromString(rs.getString("job_id")));
                    job.setJobName(rs.getString("job_type"));
                    job.setStatus(JobStatus.fromCode(rs.getString("status")));
                    return job;
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to get job - {}", e.getMessage(), e);
            return null;
        }
        return null;
    }

    public void insertJobError(String jobUUID, String message, String details) {
        UUID errorUUID = UUID.randomUUID();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "insert into job_error (job_id, error_id, error_message, stack_trace, logged_time) VALUES (?,?,?,?,NOW())"
                )
        ) {
            preparedStatement.setString(1, jobUUID);
            preparedStatement.setString(2, errorUUID.toString());
            preparedStatement.setString(3, message);
            preparedStatement.setString(4, details);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Unable to insert job error - {}", e.getMessage(), e);
        }
    }

}
