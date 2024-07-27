package org.leolo.web.nrinfo.service;


import ch.qos.logback.core.recovery.ResilientFileOutputStream;
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
import java.sql.*;
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
    private ConfigurationUtil configurationUtil;

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
            for(Line line:lines) {
                insertOrUpdateLine(line);
            }
        }
    }

    private void insertOrUpdateLine(Line line) {
        logger.debug("Inserting line {}", line.getLineName());
        // tfl_line
        try (Connection connection = dataSource.getConnection()){
            try(
                    PreparedStatement psLineChk = connection.prepareStatement(
                            "SELECT 1 FROM tfl_line WHERE line_id = ?"
                    )
            ) {
                psLineChk.setString(1, line.getLineId());
                boolean update = true;
                try (
                        ResultSet rsLineChk = psLineChk.executeQuery();
                        PreparedStatement psUpdLine = connection.prepareStatement(
                        "UPDATE tfl_line SET line_name=?, service_mode=?, is_tfl_service=? WHERE line_id=?");
                        PreparedStatement psInsLine = connection.prepareStatement(
                                "INSERT INTO  tfl_line (line_id, line_name, service_mode, is_tfl_service)" +
                                        " VALUES (?,?,?,?)"
                        )
                ) {
                    update = rsLineChk.next();
                    if (update) {
                        //Update
                        psUpdLine.setString(1, line.getLineName());
                        psUpdLine.setString(2, line.getMode().getModeName());
                        psUpdLine.setBoolean(3, line.getMode().isTflService());
                        psUpdLine.setString(4, line.getLineId());
                        psUpdLine.executeUpdate();
                    } else {
                        //Insert
                        psInsLine.setString(1, line.getLineId());
                        psInsLine.setString(2, line.getLineName());
                        psInsLine.setString(3, line.getMode().getModeName());
                        psInsLine.setBoolean(2, line.getMode().isTflService());
                        psInsLine.executeUpdate();
                    }
                }
            }
            //Remove extra records
            try(
                    PreparedStatement psRoutes = connection.prepareStatement(
                    "SELECT r.route_id, r.service_type, r.`from`, r.`to` " +
                            "from tfl_route r " +
                            "where line_id = ?");
                    PreparedStatement psRouteStopDel = connection.prepareStatement(
                            "DELETE FROM tfl_route_stop WHERE route_id = ?"
                    );
                    PreparedStatement psRouteDel = connection.prepareStatement(
                            "DELETE FROM tfl_route WHERE route_id=?"
                    )
            ) {
                psRoutes.setString(1, line.getLineId());
                int removeCount = 0;
                try (ResultSet rsRoutes = psRoutes.executeQuery()){
                    while(rsRoutes.next()){
                        boolean matched = false;
                        for(RouteSection routeSection: line.getRouteSections()){
                            if (
                                    routeSection.getServiceType().equalsIgnoreCase(rsRoutes.getString(2)) &&
                                    routeSection.getFrom().equalsIgnoreCase(rsRoutes.getString(3)) &&
                                    routeSection.getTo().equalsIgnoreCase(rsRoutes.getString(4))
                            ) {
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            logger.info("Removing route {}({},{},{})", line.getLineId(),
                                    rsRoutes.getString(2),
                                    rsRoutes.getString(3),
                                    rsRoutes.getString(4)
                            );
                            psRouteStopDel.setInt(1, rsRoutes.getInt(1));
                            psRouteStopDel.executeQuery();
                            psRouteDel.setInt(1, rsRoutes.getInt(1));
                            psRouteDel.executeQuery();
                            removeCount++;
                        }
                    }
                }
                logger.info("{} route to be removed for line {}", removeCount, line.getLineId());
            }
            //Update/Insert the routes
            try (
                    PreparedStatement psRouteSection = connection.prepareStatement(
                            "SELECT route_id FROM tfl_route " +
                                    "WHERE line_id=? AND service_type=? AND `from`=? AND `to` = ?"
                    )
            ) {
                for(RouteSection routeSection: line.getRouteSections()){
                    String routeId = null;
                    psRouteSection.setString(1, line.getLineId());
                    psRouteSection.setString(2, routeSection.getServiceType());
                    psRouteSection.setString(3, routeSection.getFrom());
                    psRouteSection.setString(4, routeSection.getTo());
                    try(ResultSet rsRouteSelection = psRouteSection.executeQuery()){
                        if(rsRouteSelection.next()){
                            routeId = rsRouteSelection.getString(1);
                        }
                    }
                    if (routeId == null){
                        logger.debug("Service Type is {}", routeSection.getServiceType());
                        //Insert
                        try(PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO tfl_route (" +
                                        "mode, line_id, service_type, `from`, `to`, " +
                                        "line_name, route_description, direction, line_string" +
                                        ") VALUES (" +
                                        "?,?,?,?,?," +
                                        "?,?,?,?" +
                                        ")",
                                Statement.RETURN_GENERATED_KEYS
                        )){
                            ps.setString(1,line.getMode().getModeName());
                            ps.setString(2, line.getLineId());
                            ps.setString(3, routeSection.getServiceType());
                            ps.setString(4, routeSection.getFrom());
                            ps.setString(5, routeSection.getTo());
                            ps.setString(6, line.getLineName());
                            ps.setString(7, routeSection.getRouteDescription());
                            ps.setString(8, routeSection.getDirection());
                            ps.setString(9, routeSection.getLineString());
                            ps.executeUpdate();
                            ResultSet rs = ps.getGeneratedKeys();
                            rs.next();
                            routeId = rs.getString(1);
                        }
                    } else {
                        //Update
                        try(
                                PreparedStatement ps = connection.prepareStatement(
                                        "UPDATE tfl_route " +
                                                "SET " +
                                                "mode = ?, line_id = ?, service_type = ?, `from` = ?, `to` = ?, " +
                                                "line_name = ?, route_description = ?, direction = ?, line_string = ? " +
                                                "WHERE route_id = ?"
                                )
                                ){

                            ps.setString(1,line.getMode().getModeName());
                            ps.setString(2, line.getLineId());
                            ps.setString(3, routeSection.getServiceType());
                            ps.setString(4, routeSection.getFrom());
                            ps.setString(5, routeSection.getTo());
                            ps.setString(6, line.getLineName());
                            ps.setString(7, routeSection.getRouteDescription());
                            ps.setString(8, routeSection.getDirection());
                            ps.setString(9, routeSection.getLineString());
                            ps.setString(10, routeId);
                            ps.executeUpdate();
                        }
                    }
                    // Update the stops
                    connection.setAutoCommit(false);
                    try(
                            PreparedStatement psDel = connection.prepareStatement(
                                    "DELETE FROM tfl_route_stop WHERE route_id = ?"
                            );
                            PreparedStatement psIns = connection.prepareStatement(
                                    "INSERT INTO  tfl_route_stop (route_id, seq, stoppoint) VALUES (?,?,?)"
                            )
                            ) {
                        psDel.setString(1, routeId);
                        psDel.executeUpdate();
                        psIns.setString(1, routeId);
                        int seq = 1;
                        for (String stopPoint:routeSection.getStopPoints()){
                            psIns.setInt(2, seq++);
                            psIns.setString(3, stopPoint);
                            psIns.addBatch();
                        }
                        psIns.executeBatch();
                        connection.commit();
                        connection.setAutoCommit(true);
                    }

                }
            }

        } catch (SQLException e) {

            logger.error("Error when insert/updating line : {}", e.getMessage(), e);
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
        for (int lineIdx=0; lineIdx<respArray.length(); lineIdx++) {
            JSONObject lineObject = respArray.getJSONObject(lineIdx);
            Line line = new Line();
            line.setMode(mode);
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
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
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
                            RouteSection routeSection = new RouteSection();
                            routeSection.setDirection(direction);
                            routeSection.setServiceType(serviceType);
                            routeSection.setLineString(lineString);
                            routeSection.setRouteDescription(stopPoints.optString("name"));
                            for (int stopIdx=0; stopIdx<naptanIds.length();stopIdx++) {
                                routeSection.getStopPoints().add(naptanIds.getString(stopIdx));
                            }
                            line.getRouteSections().add(routeSection);
                        }
                    }
                }
            }
            lines.add(line);
        }
        return lines;
    }

}
