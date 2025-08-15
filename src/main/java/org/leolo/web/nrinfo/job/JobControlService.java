package org.leolo.web.nrinfo.job;

import org.leolo.web.nrinfo.job.dao.JobDao;
import org.leolo.web.nrinfo.job.model.Job;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Service
public class JobControlService {

    private Logger logger = LoggerFactory.getLogger(JobControlService.class);

    @Autowired
    private JobDao jobDao;

    @Autowired
    private ConfigurationService configService;

    private Map<UUID, JobDataEntry> dataMap = new Hashtable<>();

    public void startJob(UUID uuid, String jobType) {
        jobDao.addJob(new Job(uuid, jobType));
    }

    public void markJobAsCompleted(UUID uuid) {
        Job job = jobDao.getJob(uuid.toString());
        if (job != null) {
            job.setStatus(JobStatus.COMPLETED);
            jobDao.updateJob(job);
            dataMap.remove(job.getJobUUID());
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
            dataMap.remove(job.getJobUUID());
        } else {
            logger.warn("Cannot find job {}", uuid);
        }

    }

    @Scheduled(fixedDelay = 120_000)
    public void purgeExpiredJobData() {
        long maxAge = Long.parseLong(configService.getConfiguration("jobdata.max_data_age","7200"));
        long maxLAD = System.currentTimeMillis() - maxAge;
        ArrayList<UUID> toDelete = new ArrayList<>();
        for(UUID uuid : dataMap.keySet()) {
            if (dataMap.get(uuid).lastAccessDate < maxLAD) {
                toDelete.add(uuid);
            }
        }
        logger.info("Purging expired job data, {} to be deleted out of {}", toDelete.size(), dataMap.keySet());
        for(UUID uuid : toDelete) {
            dataMap.remove(uuid);
        }
    }

    public <T> T getJobData(UUID jobUUID, String dataKey, T defaultValue) {
        JobDataEntry dataEntry = dataMap.get(jobUUID);
        if (dataEntry == null) {
            return null;
        }
        dataEntry.lastAccessDate = System.currentTimeMillis();

        Object value = dataEntry.data.get(dataKey);
        if (value != null) {
            return (T) value;
        } else {
            return defaultValue;
        }
    }

    public <T> T getJobData(UUID jobUUID, String dataKey) {
        return getJobData(jobUUID, dataKey, null);
    }

    public <T> void putJobData(UUID jobUUID, String dataKey, T value) {
        JobDataEntry dataEntry = dataMap.get(jobUUID);
        if (dataEntry == null) {
            dataEntry = new JobDataEntry();
            dataEntry.createdDate = System.currentTimeMillis();
            dataMap.put(jobUUID, dataEntry);
        }
        dataEntry.lastAccessDate = System.currentTimeMillis();
        dataEntry.data.put(dataKey, value);
    }

    private static class JobDataEntry{
        long createdDate;
        long lastAccessDate;

        Map<String, Object> data = new Hashtable<>();
    }
}
