package org.leolo.web.nrinfo.service;


import org.json.JSONArray;
import org.json.JSONObject;
import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.model.tfl.Line;
import org.leolo.web.nrinfo.model.tfl.RouteSection;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.leolo.web.nrinfo.model.tfl.ServiceType;
import org.leolo.web.nrinfo.util.ApiUtil;
import org.leolo.web.nrinfo.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.Vector;

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
    ConfigurationUtil configurationUtil;

    public void loadRoutes(UUID jobUUID) throws IOException {
        jobStatusService.insertJob(jobUUID, "dataload.tfl.route");
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
        //Step 1: Get a list of mode
        List<ServiceMode> modes = tfLApiService.getServiceMode();
        for(ServiceMode mode : modes) {
            logger.debug("Processing mode {}", mode.getModeName());
            //Step 2: Get a list of the line and service Type
            List<Line> lines = getLines(mode);
        }
    }

    private List<Line> getLines(ServiceMode mode) throws IOException {
        List<Line> lines = new Vector<>();
        String requestUrl = "https://api.tfl.gov.uk/line/mode/%s?app_key=%s";
        requestUrl = requestUrl.formatted(
                mode.getModeName(),
                configurationUtil.getConfigValue(Constants.PROP_TFL_PRIMARY_API_KEY)
        );
        String lineResp = ApiUtil.sendSimpleRequest(requestUrl);
        JSONArray respArray = new JSONArray(lineResp);
        if (mode.getModeName().equalsIgnoreCase("bus")){
            //For debug only
            return lines;
        }
        for (int lineIdx=0; lineIdx<respArray.length(); lineIdx++) {
            JSONObject lineObject = respArray.getJSONObject(lineIdx);
            Line line = new Line();
            line.setLineName(lineObject.optString("name"));
            line.setLineId(lineObject.optString("id"));
            JSONArray serviceTypes = lineObject.optJSONArray("serviceTypes");
            if (serviceTypes != null) {
                for (int serviceTypeIdx = 0; serviceTypeIdx < serviceTypes.length(); serviceTypeIdx++) {
                    JSONObject serviceTypeObject = serviceTypes.getJSONObject(serviceTypeIdx);
                    String serviceType = serviceTypeObject.getString("name");
                    for(String direction: new String[]{"inbound", "outbound"}) {
                        requestUrl = "https://api.tfl.gov.uk/Line/%s/Route/Sequence/%s?serviceTypes=%s&excludeCrowding=true&app_key=%s";
                        requestUrl = requestUrl.formatted(
                                line.getLineId(),
                                direction,
                                serviceType,
                                configurationUtil.getConfigValue(Constants.PROP_TFL_PRIMARY_API_KEY)
                        );
                        String routeResp = ApiUtil.sendSimpleRequest(requestUrl);
                        JSONObject routeObject = new JSONObject(routeResp);
                        JSONArray lineStringArray = routeObject.getJSONArray("lineStrings");
                        JSONArray stopPointArray = routeObject.getJSONArray("orderedLineRoutes");
                        if (lineStringArray.length() != stopPointArray.length()){
                            logger.warn("Length mismatch for linestring({}) and stoppoints({})", lineStringArray.length(), stopPointArray.length());
                        }
                        for (int i=0;i<lineStringArray.length()&&i<stopPointArray.length();i++){
                            String lineString = lineStringArray.getString(i);
                            JSONObject stopPoints = stopPointArray.getJSONObject(i);
                            String routeDescription = stopPoints.getString("name");
                            JSONArray naptanIds = stopPoints.getJSONArray("naptanIds");
                            logger.debug("Idx {} is {}, with {} NaPTANs", i, routeDescription, naptanIds.length());
                        }
                    }
                }
            }
        }
        return lines;
    }

    private List<RouteSection> parseRouteSectionObject(JSONObject object){
        List<RouteSection> routeSections = new Vector<>();

        return routeSections;
    }

}
