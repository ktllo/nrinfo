package org.leolo.web.nrinfo.service;

import org.json.JSONObject;
import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.irc.IrcService;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.leolo.web.nrinfo.util.ApiUtil;
import org.leolo.web.nrinfo.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.json.JSONArray;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "remote.tfl")
public class TfLApiService {

    private static Logger logger = LoggerFactory.getLogger(TfLApiService.class);

    @Autowired
    ConfigurationUtil configurationUtil;

    @Autowired
    IrcService ircService;

    @Autowired
    private DataSource dataSource;

    public List<ServiceMode> getServiceMode() throws IOException {
        String requestUrl = "https://api.tfl.gov.uk/Line/Meta/Modes?app_key=%s";
        requestUrl = requestUrl.formatted(configurationUtil.getConfigValue(Constants.PROP_TFL_PRIMARY_API_KEY));
        String modeResp = ApiUtil.sendSimpleRequest(requestUrl);
        List<ServiceMode> returnList = new Vector<>();
        JSONArray jsonArray = new JSONArray(modeResp);
        logger.debug("Expecting {} modes", jsonArray.length());
        for(int i=0;i< jsonArray.length();i++){
            JSONObject entry = jsonArray.getJSONObject(i);
            returnList.add(new ServiceMode(
                    entry.optString("modeName"),
                    entry.optBoolean("isTflService", false),
                    entry.optBoolean("isFarePaying", false),
                    entry.optBoolean("isScheduledService", false)
            ));
        }
        return returnList;
    }
    public List<LineStatus> getModeStatus(List<ServiceMode> modes) throws IOException {
        Vector<LineStatus> statusList = new Vector<>();
        Map<String, List<LineStatus>> map = getModeStatusByLine(modes);
        for (String key : map.keySet()) {
            statusList.addAll(map.get(key));
        }
        return statusList;
    }

    public Map<String,List<LineStatus>> getModeStatusByLine(List<ServiceMode> modes) throws IOException {
        if(modes.size()==0){
            //Given mode list is empty, let's be lazy and return an empty list
            return new Hashtable<>();
        }
        logger.debug("List size is {}", modes.size());
        //Build the modeString
        StringBuilder modeString = new StringBuilder();
        Iterator<ServiceMode> iModes = modes.iterator();
        while(true){
            modeString.append(iModes.next().getModeName());
            if(iModes.hasNext()){
                modeString.append(",");
            } else {
                break;
            }
        }
        logger.debug("Mode string is {}", modeString);
        String requestUrl = "https://api.tfl.gov.uk/line/mode/%s/status?app_key=%s&detail=true";
        requestUrl = requestUrl.formatted(
                modeString.toString(),
                configurationUtil.getConfigValue(Constants.PROP_TFL_PRIMARY_API_KEY)
        );
        String statusResp = ApiUtil.sendSimpleRequest(requestUrl);
//        List<LineStatus> lineStatuses = new Vector<>();
        Map<String,List<LineStatus>> statusMap = new Hashtable<>();
        JSONArray rootArray = new JSONArray(statusResp);
        for(int lineIdx=0;lineIdx< rootArray.length();lineIdx++){
            JSONObject lineObject = rootArray.getJSONObject(lineIdx);
            //We need the array lineStatuses
            JSONArray statuses = lineObject.optJSONArray("lineStatuses");
            String lineName = lineObject.optString("name");
            String lineId = lineObject.optString("id");
            logger.debug("Line {} with id {} have {} status",
                    lineName, lineId, statuses.length());
            for(int statusIdx=0;statusIdx<statuses.length();statusIdx++){
                JSONObject status = statuses.getJSONObject(statusIdx);
                int statusSeverity = status.optInt("statusSeverity");
                String statusSeverityDescription = status.optString("statusSeverityDescription");
                if (statusSeverity != 10) {
                    logger.debug("Line {} has {} ({})", lineName, statusSeverityDescription, statusSeverity);
                    LineStatus lineStatus = new LineStatus();
                    lineStatus.setLine(lineId);
                    lineStatus.setStatusSeverity(statusSeverity);
                    lineStatus.setStatusSeverityDescription(statusSeverityDescription);
                    //Process validity period
                    long start = Long.MAX_VALUE;
                    long end = Long.MIN_VALUE;
                    JSONArray validityPeriods = status.optJSONArray("validityPeriods");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    for (int vpIdx=0;vpIdx<validityPeriods.length();vpIdx++) {
                        JSONObject period = validityPeriods.getJSONObject(vpIdx);
                        try {
                            long cStart = sdf.parse(period.getString("fromDate")).getTime();
                            long cEnd = sdf.parse(period.getString("toDate")).getTime();
                            if(cStart<start){
                                start = cStart;
                            }
                            if(cEnd>end){
                                end = cEnd;
                            }
                        } catch (ParseException e) {
                            logger.warn("Unable to parse date - {}", e.getMessage(), e);
                        }
                    }
                    logger.debug("Validity period is {} - {}", new Date(start), new Date(end));
                    String reason = status.optString("reason");
                    logger.debug("Reason is {}", reason);
                    lineStatus.setReason(reason);
                    if (start != Long.MAX_VALUE)
                        lineStatus.setFromTime(new Date(start));
                    if(end != Long.MIN_VALUE)
                        lineStatus.setToTime(new Date(end));
                    //TODO: Add the line and station data
                    JSONObject disruption = status.optJSONObject("disruption");
                    if (disruption != null) {
                        JSONArray affectedRoutes = disruption.optJSONArray("affectedRoutes");
                        if (affectedRoutes != null) {
                            for (int aRouteIdx=0;aRouteIdx<affectedRoutes.length();aRouteIdx++) {
                                JSONObject aRoute = affectedRoutes.getJSONObject(aRouteIdx);
                                String routeId = aRoute.optString("id");
                                lineStatus.addRoute(routeId);
                            }
                        }
                        JSONArray affectedStops = disruption.optJSONArray("affectedStops");
                        if (affectedStops != null) {
                            for (int aStopIdx=0;aStopIdx<affectedStops.length();aStopIdx++) {
                                JSONObject stop = affectedStops.getJSONObject(aStopIdx);
                                String stopId = stop.optString("naptanId");
                                lineStatus.addStation(stopId);
                            }
                        }
                    } else {
                        logger.debug("No disruption detail found.");
                    }
                    lineStatus.setMode(lineObject.optString("modeName"));
                    if (statusMap.containsKey(lineId)){
                        statusMap.get(lineId).add(lineStatus);
                    } else {
                        Vector<LineStatus> lineVector = new Vector<>();
                        lineVector.add(lineStatus);
                        statusMap.put(lineId, lineVector);
                    }
                } else {
                    //Good Service
                    LineStatus lineStatus = new LineStatus();
                    lineStatus.setLine(lineId);
                    lineStatus.setStatusSeverity(statusSeverity);
                    lineStatus.setStatusSeverityDescription(statusSeverityDescription);
                    lineStatus.setMode(lineObject.optString("modeName"));
                    if (statusMap.containsKey(lineId)){
                        statusMap.get(lineId).add(lineStatus);
                    } else {
                        Vector<LineStatus> lineVector = new Vector<>();
                        lineVector.add(lineStatus);
                        statusMap.put(lineId, lineVector);
                    }
                }
            }

        }
        return statusMap;
    }

    public String getLineName(String id) {
        try(
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT line_name FROM tfl_line WHERE line_id = ?"
                )
        ) {
            ps.setString(1, id);
            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to get line name for id {} : {}", id, e.getMessage(), e);
        }
        return null;
    }
}
