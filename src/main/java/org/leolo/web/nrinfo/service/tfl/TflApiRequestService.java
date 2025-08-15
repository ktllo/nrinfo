package org.leolo.web.nrinfo.service.tfl;

import io.micrometer.core.instrument.util.IOUtils;
import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.leolo.web.nrinfo.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class TflApiRequestService {
    private static TflDataLoadService _instant;

    private Logger logger = LoggerFactory.getLogger(TflApiRequestService.class);

    private long lastRequestTime = 0;

    private int REQUEST_INTERVAL = -1;

    @Autowired
    private ConfigurationUtil configurationUtil;
    @Autowired
    private ConfigurationService configurationService;

    //Empty constructor to prevent another instance from created
    public TflApiRequestService() {
    }

    public String sendRequest(String url) throws IOException {
        return sendRequest(url, Collections.emptyMap());
    }

    public String sendRequest(String url, Map<String, String> parameters) throws IOException {
        if (REQUEST_INTERVAL == -1) {
            REQUEST_INTERVAL = Integer.parseInt(configurationUtil.getConfigValue("remote.tfl.request_interval"));
        }
        if (parameters != null) {
            //Make sure that the map is mutable
            parameters = new HashMap<>(parameters);
        } else {
            parameters = new HashMap<>();
        }
        // Check is the request starts with http, if so, verify that it starts with https://api.tfl.gov.uk
        if (url.startsWith("http")){
            if (!url.startsWith("https://api.tfl.gov.uk")) {
                //Not our request
                throw new RuntimeException("url does not start with https://api.tfl.gov.uk");
            }
        } else if (!url.startsWith("/")) {
            url = "https://api.tfl.gov.uk/"+url;
        } else {
            url = "https://api.tfl.gov.uk"+url;
        }
        if (parameters.containsKey("app_key")) {
            logger.warn("app_key given in map, it may be incorrect");
        } else {
            parameters.put("app_key", configurationService.getConfigurationGroup("key.tfl"));
        }
        boolean hadEntry = false;
        StringBuilder sb = new StringBuilder(url);
        if (url.contains("?")){
            hadEntry = true;
            logger.warn("url contains query parameters");
        } else {
            sb.append("?");
        }
        for (String key : parameters.keySet()) {
            if (hadEntry) {
                sb.append("&");
            }
            sb.append(key).append("=").append(parameters.get(key));
            hadEntry = true;
        }
        logger.info("Request URL is {}", sb.toString());
        return doRequest(sb.toString());
    }

    public long getLastRequestTime() {
        return lastRequestTime;
    }

    public Date getLastRequestDate(){
        return new Date(lastRequestTime);
    }

    private synchronized String doRequest(String url) throws IOException {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastRequestTime;
        if (diff < REQUEST_INTERVAL) {
            logger.warn("Request are too close to each other. Only {} ms.", diff);
            try {
                synchronized (this) {
                    Thread.sleep(REQUEST_INTERVAL - diff);
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
            }
        }
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        long startTime = System.currentTimeMillis();
        con.connect();
        long endTime = System.currentTimeMillis();
        logger.info("Request took {} ms", endTime - startTime);
        lastRequestTime = endTime;
        int responseCode = con.getResponseCode();
        InputStream inputStream = (responseCode > 299)  ? con.getErrorStream() : con.getInputStream();
        String resp = IOUtils.toString(inputStream);
        logger.info(Constants.MARKER_EXTERNAL_REQ, "tfl,{},{}", resp.length(), endTime - startTime);
        return resp;
    }
}
