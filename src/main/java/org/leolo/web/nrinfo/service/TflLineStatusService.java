package org.leolo.web.nrinfo.service;

import lombok.Getter;
import org.leolo.web.nrinfo.irc.IrcService;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;

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
    @Autowired
    private IrcService ircService;

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
        //TODO: Compare the map on another thread
        compareStatusMap(lineStatusMap, currentStatus);
        //Replacing the map
        synchronized (LOCK) {
            lineStatusMap = currentStatus;
            lastUpdate = new Date();
        }
    }

    private void compareStatusMap(
            final Map<String, List<LineStatus>> oldMap,
            final Map<String, List<LineStatus>> newMap
    ) {
        new Thread(()->{
            List<String> updateList = new ArrayList<>();
            for(String lineId: newMap.keySet()){
                //Step 1: find new line
                //Step 2: Compare line by line
                if (!oldMap.containsKey(lineId)) {
                    logger.debug("-->-->-->NEW LINE : {}", lineId);
                    //TODO: Update database
                } else {
                    List<LineStatus> newStatuses = newMap.get(lineId);
                    List<LineStatus> oldStatuses = oldMap.get(lineId);
                    //Check is the only status "Good Service"
                    if (newStatuses.size()==1 && newStatuses.get(0).getStatusSeverity()==10) {
                        //TODO: Update database
                        if (oldStatuses.size()>1 || (oldStatuses.size()==1 && oldStatuses.get(0).getStatusSeverity()!=10)) {
                            updateList.add("There are Good Service now on " + tfLApiService.getLineName(lineId));
                        }
                        logger.debug("{}: -->--> GOOD SERVICE", lineId);
                    }
                    Set<String> newLevel = new TreeSet<>();
                    Set<String> removedLevel = new TreeSet<>();
                    //Check for new status
                    for(LineStatus newStatus:newStatuses) {
                        boolean inOld = false;
                        for(LineStatus oldStatus:oldStatuses){
                            if(newStatus.getStatusSeverity()==oldStatus.getStatusSeverity()){
                                inOld = true;
                                break;
                            }
                        }
                        if (!inOld) {
                            newLevel.add(newStatus.getStatusSeverityDescription());
                        }
                    }
                    //Check for missing old status
                    for(LineStatus oldStatus:oldStatuses) {
                        boolean inNew = false;
                        for(LineStatus newStatus:newStatuses){
                            if(newStatus.getStatusSeverity()==oldStatus.getStatusSeverity()){
                                inNew = true;
                                break;
                            }
                        }
                        if (!inNew) {
                            removedLevel.add(oldStatus.getStatusSeverityDescription());
                        }
                    }
                    //form the message
                    if (newLevel.size()>0||removedLevel.size()>0) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(tfLApiService.getLineName(lineId)).append(" is ");
                        if (newLevel.size() > 0) {
                            sb.append("now having ");
                            for (String status : newLevel) {
                                sb.append(status).append(" ");
                            }
                        }
                        if (removedLevel.size() > 0) {
                            if (newLevel.size() > 0) {
                                sb.append("and ");
                            }
                            sb.append("no longer ");
                            for (String status : removedLevel) {
                                sb.append(status).append(" ");
                            }
                        }
                        updateList.add(sb.toString());
                    }
                }
            }
            if(updateList.size()>0){
                ircService.sendMessage("This is an update from the control corner of #traintalk");
                for(String update:updateList){
                    ircService.sendMessage(update);
                }
                ircService.sendMessage("This is the end of the update from the control room.");
            }
        }).start();
    }

    public Map<String, List<LineStatus>> getLineStatusMap() {
        synchronized (LOCK) {
            return lineStatusMap;
        }
    }

}
