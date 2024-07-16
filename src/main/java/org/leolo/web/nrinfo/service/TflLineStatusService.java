package org.leolo.web.nrinfo.service;

import lombok.Getter;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TflLineStatusService {

    private Logger logger = LoggerFactory.getLogger(TflLineStatusService.class);

    @Autowired
    private DataSource dataSource;

    private Map<String, List<LineStatus>> lineStatusMap = new HashMap<String, List<LineStatus>>();

    @Getter
    private Date lastUpdate = null;

    private final Object LOCK = new Object();

    @Autowired
    private TfLApiService tfLApiService;
    public void execute() throws IOException {
        logger.info("Job started");
        //Step 1: Get the list of modes
        List<ServiceMode> modes = tfLApiService.getServiceMode();
        //Step 2: Fetch the status
        Map<String, List<LineStatus>> currentStatus = tfLApiService.getModeStatusByLine(
                modes.stream()
                        .filter((e)->e.isTflService())
                        .filter(e->!e.getModeName().equalsIgnoreCase("bus")) //We don't care about bus here
                        .toList());
        //TODO: Compare the map

        //Replacing the map
        synchronized (LOCK) {
            lineStatusMap = currentStatus;
            lastUpdate = new Date();
        }
    }

    public Map<String, List<LineStatus>> getLineStatusMap() {
        synchronized (LOCK) {
            return lineStatusMap;
        }
    }

}
