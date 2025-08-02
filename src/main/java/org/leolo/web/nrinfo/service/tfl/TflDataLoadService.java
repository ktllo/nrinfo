package org.leolo.web.nrinfo.service.tfl;

import org.json.JSONArray;
import org.json.JSONObject;
import org.leolo.web.nrinfo.dao.tfl.LineDao;
import org.leolo.web.nrinfo.dao.tfl.RouteDao;
import org.leolo.web.nrinfo.dao.tfl.ServiceModeDao;
import org.leolo.web.nrinfo.model.tfl.Line;
import org.leolo.web.nrinfo.model.tfl.Route;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TflDataLoadService {
    public static final Object SYNC_TOKEN_DATALOAD = new Object();
    private Logger logger = LoggerFactory.getLogger(TflDataLoadService.class);

    @Autowired
    private ServiceModeDao serviceModeDao;

    @Autowired
    private TflApiRequestService tflApiRequestService;

    @Autowired
    private LineDao lineDao;

    @Autowired
    private RouteDao routeDao;

    public void loadRoutes(UUID jobUUID) throws IOException {
        logger.info("Loading routes for job {}", jobUUID);
        List<ServiceMode> serviceModeList = serviceModeDao.getAllServiceMode();
        for (ServiceMode serviceMode : serviceModeList) {
            logger.debug("Handling service mode {}", serviceMode.getModeName());
            loadRoutesForServiceMode(jobUUID, serviceMode, "Regular");
//            loadRoutesForServiceMode(jobUUID, serviceMode, "Night");
        }
    }

    private void loadRoutesForServiceMode(UUID jobUUID, ServiceMode serviceMode, String serviceTypes) throws IOException {
        // API call is /Line/Mode/{modes}/Route[?serviceTypes]
        String data = tflApiRequestService.sendRequest(
                "/Line/Mode/"+serviceMode.getModeCode()+"/Route",
                Map.of("serviceTypes",serviceTypes)
        );
        JSONArray lineArray = new JSONArray(data);
        for (int i = 0; i < lineArray.length(); i++) {
            JSONObject lineObject = lineArray.getJSONObject(i);
            logger.debug("Line {}", lineObject.optString("id"));
            Line line = new Line();
            line.setLineId(lineObject.optString("id"));
            line.setLineName(lineObject.optString("name"));
            line.setModeCode(lineObject.optString("modeName"));
            lineDao.insertOrUpdateLine(line);
            JSONArray routeArray = lineObject.optJSONArray("routeSections");
            for (int j = 0; j < routeArray.length(); j++) {
                JSONObject routeObject = routeArray.getJSONObject(j);
                logger.debug("Route code = {}", routeObject.optString("routeCode"));
                Route route = new Route();
                route.setLineId(line.getLineId());
                route.setDirection(routeObject.optString("direction"));
                route.setOrigin(routeObject.optString("originator"));
                route.setDestination(routeObject.optString("destination"));
                routeDao.insertOrUpdateRoute(route);
            }
        }
    }
}
