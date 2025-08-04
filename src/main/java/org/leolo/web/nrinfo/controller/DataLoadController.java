package org.leolo.web.nrinfo.controller;

import org.leolo.web.nrinfo.service.ConfigurationService;
import org.leolo.web.nrinfo.service.networkrail.NetworkRailDataLoadService;
import org.leolo.web.nrinfo.service.tfl.TflDataLoadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController()
@RequestMapping("/api/v1/admin/dataload")
public class DataLoadController {

    private final Logger logger = LoggerFactory.getLogger(DataLoadController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private TflDataLoadService tflDataLoadService;

    @Autowired
    private NetworkRailDataLoadService networkRailDataLoadService;

    @RequestMapping("tfl/route")
    public Object loadTflRoute(
            @RequestParam(name = "key", required = false, defaultValue = "") String key
    ){
        String targetKey = configurationService.getConfiguration("dataload.tfl.key");
        if(!targetKey.trim().equals(key.trim())){
            //Given key is invalid
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("status","error");
            errorMap.put("message","Unauthorized");
            return errorMap;
        }
        //We start the job
        final UUID jobUUID = UUID.randomUUID();
        new Thread(()->{
            synchronized (TflDataLoadService.SYNC_TOKEN_DATALOAD){
                try {
                    tflDataLoadService.loadRoutes(jobUUID);
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
            }
        }).start();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("status","success");
        resultMap.put("jobId",jobUUID.toString());
        resultMap.put("message","Job started");
        return resultMap;
    }

    @RequestMapping("networkrail/{type}")
    public Object loadNetworkRailData(
            @RequestParam(name = "key", required = false, defaultValue = "") String key,
            @PathVariable String type) {
        String targetKey = configurationService.getConfiguration("dataload.networkrail.key");
        if(!targetKey.trim().equals(key.trim())){
            //Given key is invalid
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("status","error");
            errorMap.put("message","Unauthorized");
            return errorMap;
        }
        //Look for the dataload method
        Method dataloadMethod = networkRailDataLoadService.getLoaderMethod(type);
        if (dataloadMethod == null) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status","error");
            resultMap.put("message","Type not implemented");
            resultMap.put("type",type);
            return resultMap;
        }
        //We start the job
        final UUID jobUUID = UUID.randomUUID();
        new Thread(()->{
            synchronized (TflDataLoadService.SYNC_TOKEN_DATALOAD){
                try {
                    dataloadMethod.invoke(networkRailDataLoadService, jobUUID);
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
            }
        }).start();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("status","success");
        resultMap.put("jobId",jobUUID.toString());
        resultMap.put("message","Job started");
        return resultMap;
    }

}
