package org.leolo.web.nrinfo.job;

import org.leolo.web.nrinfo.job.dao.JobDao;
import org.leolo.web.nrinfo.job.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@Service
public class JobControlService {

    private Logger logger = LoggerFactory.getLogger(JobControlService.class);

    @Autowired
    private JobDao jobDao;

    public void startJob(UUID uuid, String jobType) {
        jobDao.addJob(new Job(uuid, jobType));
    }

    public void markJobAsCompleted(UUID uuid) {
        Job job = jobDao.getJob(uuid.toString());
        if (job != null) {
            job.setStatus(JobStatus.COMPLETED);
            jobDao.updateJob(job);
        } else {
            logger.warn("Cannot find job {}", uuid);
        }
    }

    public void markJobAsFailed(UUID uuid, Throwable throwable) {
        Job job = jobDao.getJob(uuid.toString());
        if (job != null) {
            job.setStatus(JobStatus.FAILED);
            jobDao.updateJob(job);
            String message = throwable.getMessage();
            String details = null;
            try (
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintWriter pw = new PrintWriter(baos);
            ){
                throwable.printStackTrace(pw);
                pw.flush();
                details = baos.toString();
            } catch (IOException e) {
                logger.warn("Unable to get stacktrace for error - {}", e.getMessage(), e);
            }
            jobDao.insertJobError(job.getJobUUID().toString(), message, details);
        } else {
            logger.warn("Cannot find job {}", uuid);
        }

    }

}
