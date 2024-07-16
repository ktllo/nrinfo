package org.leolo.web.nrinfo.controller;

import geotrellis.proj4.CRS;
import geotrellis.proj4.Transform;
import org.leolo.web.nrinfo.service.PostcodeService;
import org.leolo.web.nrinfo.service.StopPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import scala.Tuple2;
import scala.Tuple3;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/stoppoint")
public class StopPointController {

    private Logger logger = LoggerFactory.getLogger(StopPointController.class);

    @Autowired
    private StopPointService stopPointService;
    @Autowired
    private PostcodeService postcodeService;

    @RequestMapping("near/{postcode}")
    public Object getStopPointNearPostcode(
            @PathVariable("postcode") String postcode,
            @RequestParam(name = "maxDist", required = false, defaultValue = "200") int maxDistance
    ){
        if(maxDistance<0||maxDistance>1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Distance out of range");
        }
        Tuple3<Integer,Integer, Integer> location = postcodeService.getPostcodeLocation(postcode);
        if(location==null){
            //return error
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("status","error");
            errorMap.put("message","Postcode "+postcode+" not found.");
            return errorMap;
        } else {
            return stopPointService.findStopPointNear(location._1(), location._2(), maxDistance+ location._3(), false);
        }
    }

    @RequestMapping("near/{x}/{y}")
    public Object getStopPointFromPoint(
            @PathVariable("x") double x,//Easting or latitude
            @PathVariable("y") double y,//Northing or longitude
            @RequestParam(name = "maxDist", required = false, defaultValue = "200") int maxDistance,
            @RequestParam(name = "format", required = false, defaultValue = "AUTO") String format
    ){
        if (format.equalsIgnoreCase("AUTO")) {
            logger.debug("Request format: AUTO");
            if (x>=30_000&&x<=750_000&&y>=5_000&&y<=1_220_000) {
                format ="UKOS";
            } else if ( y >= -9.01 && y <= 2.01 && x >= 49.75 && x <= 61.01){
                format = "WGS84";
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot auto detect format");
            }
        }if (format.equalsIgnoreCase("UKOS")) {
            logger.debug("Request format: UKOS");
        } else if (format.equalsIgnoreCase("WGS84")) {
            logger.debug("Request format: WGS84");
            var fromWgs84 = Transform.apply(CRS.fromEpsgCode(4326), CRS.fromEpsgCode(27700));
            Tuple2<Object, Object> ukos = fromWgs84.apply(y,x);
            logger.debug("WGS84({},{}) -> UKOS({},{})",x,y,ukos._1(), ukos._2());
            x=(int) Math.round((Double) ukos._1());
            y=(int) Math.round((Double) ukos._2());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location format");
        }
        if(maxDistance<0||maxDistance>1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Distance out of range");
        }
        return stopPointService.findStopPointNear((int)Math.round(x),(int)Math.round(y),maxDistance, false);
    }

}
