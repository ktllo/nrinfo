package org.leolo.web.nrinfo.job;

import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.service.TflLineStatusService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TflLineStatusJob {

    private Logger log = LoggerFactory.getLogger(TflLineStatusJob.class);

    @Autowired
    private TflLineStatusService tflLineStatusService;

    @Scheduled(fixedDelay = 300_000) //300s = 5 min
    public void execute() throws JobExecutionException, IOException {
        log.info("Job triggered");
        tflLineStatusService.execute();
    }
}
