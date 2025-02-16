package org.leolo.web.nrinfo.web.controller;

import lombok.Getter;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.service.TflLineService;
import org.leolo.web.nrinfo.service.TflLineStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class TflStatusWebController {

    private Logger logger = LoggerFactory.getLogger(TflStatusWebController.class);

    @Autowired
    private TflLineStatusService tflLineStatusService;

    @Autowired
    private TflLineService tflLineService;

    @RequestMapping("/tfl/status")
    public String status(Model model){
        Map<String, List<LineStatus>> statusMap = tflLineStatusService.getLineStatusMap();
        ArrayList<BasicDisplayBean> priorityList = new ArrayList<>();
        ArrayList<BasicDisplayBean> standardList = new ArrayList<>();
        ArrayList<BasicDisplayBean> goodServiceList = new ArrayList<>();
        // Fill in the list
        for (String lineId: statusMap.keySet()){
            BasicDisplayBean bdb = new BasicDisplayBean();
            List<LineStatus> statusList = statusMap.get(lineId);
            String lineName = tflLineService.getLineNameById(lineId);
            logger.info("Processing line {}({})", lineId, lineName);
            bdb.lineName = lineName;
            bdb.lineId = lineId;
            bdb.eventId = tflLineStatusService.getEventId(lineId);
            logger.debug("EVENT ID FOR {} : {}", lineId, bdb.eventId);
            if (statusList.size()==1 && statusList.get(0).getStatusSeverity() == 10) {
                // This is Good Service
                bdb.statusName.add("Good Service");
                bdb.goodService = true;
                goodServiceList.add(bdb);
            } else {
                // We have to further parse it!
                for(LineStatus lineStatus: statusList){
                    if(lineStatus.getStatusSeverity() <= 6){
                        bdb.important = true;
                    }
                    bdb.statusName.add(lineStatus.getStatusSeverityDescription());
                    if (bdb.statusMessage == null) {
                        bdb.statusMessage = lineStatus.getReason();
                        bdb.fromDate = lineStatus.getFromTime();
                        bdb.toDate = lineStatus.getToTime();
                    } else {
                        //Append it!
                        bdb.statusMessage = bdb.statusMessage + "<br>" + lineStatus.getReason();
                        if (lineStatus.getFromTime().before(bdb.fromDate))
                            bdb.fromDate = lineStatus.getFromTime();
                        if (lineStatus.getToTime().after(bdb.toDate))
                            bdb.toDate = lineStatus.getToTime();
                    }
                }
                if (bdb.important) {
                    priorityList.add(bdb);
                } else {
                    standardList.add(bdb);
                }
            }
        }
        // Sort the list
        priorityList.sort(basicDisplayBeanComparator);
        standardList.sort(basicDisplayBeanComparator);
        goodServiceList.sort(basicDisplayBeanComparator);
        String lastUpdateString = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(tflLineStatusService.getLastUpdate());
        model.addAttribute("lastUpdate", lastUpdateString);
        model.addAttribute("priorityList", priorityList);
        model.addAttribute("standardList", standardList);
        model.addAttribute("goodServiceList", goodServiceList);
        logger.info("Last update : {}", lastUpdateString);
        return "tfl_status_main";
    }

    @RequestMapping("/tfl/status/{eventId}")
    private String getDisruptionHistory(@PathVariable("eventId") String eventId, Model model) {
        return "error";
    }

    @Getter
    public static class BasicDisplayBean {
        private String lineName;
        private boolean important;
        private boolean goodService;
        private TreeSet<String> statusName = new TreeSet<>();
        private String statusMessage = null;
        private Date fromDate = null;
        private Date toDate = null;
        private String lineId;
        private String eventId;

        public String getEventId(){
            return eventId==null?"":eventId;
        }
    }

    // We only concern about the line name
    private static Comparator<BasicDisplayBean> basicDisplayBeanComparator = new Comparator<BasicDisplayBean>() {
        @Override
        public int compare(BasicDisplayBean o1, BasicDisplayBean o2) {
            return o1.getLineName().compareTo(o2.getLineName());
        }
    };

}
