package org.leolo.web.nrinfo.irc.controller;

import ch.qos.logback.core.encoder.EchoEncoder;
import org.apache.commons.collections.map.HashedMap;
import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.service.TfLApiService;
import org.leolo.web.nrinfo.service.TflLineService;
import org.leolo.web.nrinfo.service.TflLineStatusService;
import org.pircbotx.Colors;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@IrcController
public class TflStatusController {

    private Logger logger = LoggerFactory.getLogger(TflStatusController.class);

    @Autowired
    private TflLineStatusService tflLineStatusService;

    @Autowired
    private TfLApiService tfLApiService;

    @Autowired
    private TflLineService tflLineService;

    @Command("lustatus")
    public void getTflStatus(PrivateMessageEvent event){
        logger.debug("lustatus.private called");
        String [] tokens = event.getMessage().split(" ");
        if (tokens.length==1){
            showPublicSummary(event);
        } else {
            showPublicDetails(event, tokens);
        }
    }

    @Command("lustatus")
    public void getTflStatus(MessageEvent event){
        logger.debug("lustatus.public called");
        String [] tokens = event.getMessage().split(" ");
        if (tokens.length==1){
            showPublicSummary(event);
        } else {
            showPublicDetails(event, tokens);
        }
    }

    private void handleShowDetails(GenericMessageEvent event, List<String> ids) {
        Map<String, List<LineStatus>> statusMap = tflLineStatusService.getLineStatusMap();
        Date lastUpdate = tflLineStatusService.getLastUpdate();
        event.respondWith(getLastUpdMessage(lastUpdate));
        TreeSet<String> respTree = new TreeSet<>();
        for(String lineId: statusMap.keySet()){
            if(ids.contains(lineId)) {
                for(LineStatus ls:statusMap.get(lineId)){
                    if (ls.getReason()!=null) {
                        respTree.add(ls.getReason());
                    } else if (ls.getStatusSeverity()==10){
                        //Good Service
                        respTree.add("There are good service on "+tflLineService.getLineNameById(lineId));
                    }
                }
            }
        }
        for(String line:respTree) {
            event.respondWith(line);
        }
        event.respondWith("This is the end of the update from the control corner");
    }

    private void showPublicDetails(GenericMessageEvent event, String [] tokens){
        if (tokens.length<2) {
            return;
        }
        int maxItems;
        if (event instanceof PrivateMessageEvent) {
            maxItems = Integer.MAX_VALUE;
        } else {
            maxItems = 2;
        }
        logger.debug("1space:{}, substr:{}", event.getMessage().indexOf(" "), event.getMessage().substring(event.getMessage().indexOf(" ")));
        List<String> ids = tflLineService.getLineIdsByPartialLineName(event.getMessage().substring(event.getMessage().indexOf(" "))).stream().filter(
                id -> {
                    if (id.equalsIgnoreCase("bus") || id.equalsIgnoreCase("national-rail"))
                        return false;
                    return true;
                }
        ).toList();
        if (ids.size()==0) {
            event.respondWith("No matching line");
        } else if (ids.size()<=maxItems) {
            handleShowDetails(event, ids);
        } else {
            ids = tflLineService.getLineIdsByLineName(event.getMessage().substring(event.getMessage().indexOf(" "))).stream().filter(
                    id -> {
                        if (id.equalsIgnoreCase("bus") || id.equalsIgnoreCase("national-rail"))
                            return false;
                        return true;
                    }
            ).toList();
            if (ids.size() > 0) {
                handleShowDetails(event, ids);
            } else {
                event.respondWith("Too many matching lines");
            }
        }

    }

    private String getLastUpdMessage(Date lastUpdate){

        StringBuilder sbLastUpd = new StringBuilder();
        sbLastUpd.append("This is an update from the control corner of #traintalk as of ").append(new SimpleDateFormat("HH:mm:ss").format(lastUpdate));
        long diffS = System.currentTimeMillis() - lastUpdate.getTime();
        if (diffS >= 300_000){
            sbLastUpd.append(Colors.BOLD).append(Colors.RED);
        }
        sbLastUpd.append(" (");
        if (diffS > 60000) {
            sbLastUpd.append(diffS/60000).append("m ");
        }
        sbLastUpd.append((diffS%60000)/1000).append("s ago)");
        return sbLastUpd.toString();
    }

    private void showPublicSummary(GenericMessageEvent event){
        Map<String, List<LineStatus>> statusMap = tflLineStatusService.getLineStatusMap();
        Date lastUpdate = tflLineStatusService.getLastUpdate();
        Map<String, Set<String>> summaryMap = new HashMap<>();
        for(String line: statusMap.keySet()){
            for(LineStatus status: statusMap.get(line)){
                logger.debug("{} -> {}({})", line, status.getStatusSeverityDescription(), status.getStatusSeverity());
                if (status.getStatusSeverity()==10){
                    continue;
                }
                if(!summaryMap.containsKey(status.getStatusSeverityDescription())){
                    summaryMap.put(status.getStatusSeverityDescription(), new TreeSet<>());
                }
                summaryMap.get(status.getStatusSeverityDescription()).add(line);
            }
        }
        event.respondWith(getLastUpdMessage(lastUpdate));
        if (summaryMap.size()==0) {
            event.respondWith("Good service on all lines");
        } else {
            for (String status : summaryMap.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(Colors.BOLD).append(status).append(Colors.NORMAL).append(": ");
                List<String> tList = new ArrayList<>();
                for (String line : summaryMap.get(status)) {
                    String name = tfLApiService.getLineName(line);
                    if (name != null) {
                        tList.add(name);
                    }
                }
                sb.append(String.join(", ", tList));
                event.respondWith(sb.toString());
            }
            event.respondWith("Good service on all other lines");
        }
    }
}
