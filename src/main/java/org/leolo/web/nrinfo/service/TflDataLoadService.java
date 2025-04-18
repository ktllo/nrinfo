package org.leolo.web.nrinfo.service;


import org.leolo.web.nrinfo.job.dao.JobErrorDao;
import org.leolo.web.nrinfo.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Component
public class TflDataLoadService {

    public static final Object SYNC_TOKEN_DATALOAD = new Object();

    private Logger logger = LoggerFactory.getLogger(TflDataLoadService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TfLApiService tfLApiService;

    @Autowired
    private JobStatusService jobStatusService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ConfigurationUtil configurationUtil;

    @Autowired
    private JobErrorDao jobErrorDao;

    private UUID currentJobId;

    public void loadRoutes(UUID jobUUID) throws IOException {
        jobStatusService.insertJob(jobUUID, "dataload.tfl.route");
        currentJobId = jobUUID;
        //Step 0: Check last run
        Date lastRunTime = jobStatusService.getLastRunTime("tfl.route");
        if (lastRunTime == null) {
            //We need to get the value from the database
            long minInterval = Long.parseLong(configurationService.getConfiguration("dataload.tfl.min_interval","86400")) * 1000;
            long diffTime = System.currentTimeMillis() - lastRunTime.getTime();
            logger.info("Difference is {} ms, {} ms required", diffTime, minInterval);
            if (diffTime < minInterval) {
                jobStatusService.markJobFailed(jobUUID, "Not enough time from last execution");
                return;
            }
        }

    }


}
