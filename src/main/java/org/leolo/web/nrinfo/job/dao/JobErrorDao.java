package org.leolo.web.nrinfo.job.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class JobErrorDao {
    @Autowired
    private DataSource ds;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void logError(UUID jobId,UUID errorId, Throwable e) {
        try (
                Connection conn = ds.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO job_error (job_id, error_id, exception, stacktrace, logged_time) " +
                                "VALUES (?, ?, ?, ?, NOW())")
        ) {
            ps.setString(1, jobId.toString());
            ps.setString(2, errorId.toString());
            ps.setString(3, e.toString());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            ps.setString(4, sw.toString());
            ps.executeUpdate();
        } catch (SQLException err) {
            logger.error("Unable to log error - {}", err.getMessage(), err);
        }
    }
    public void logError(UUID jobId, Throwable e) {
        logError(jobId, UUID.randomUUID(), e);
    }

}
