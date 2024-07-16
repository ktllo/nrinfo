package org.leolo.web.nrinfo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ApiUtil {

    private static Logger logger = LoggerFactory.getLogger(ApiUtil.class);

    public static String sendSimpleRequest(String url) throws IOException {
        return sendSimpleRequest(new URL(url));
    }
    public static String sendSimpleRequest(URL url) throws IOException {
        logger.debug("Sending GET request to {}", url.toString());
        URLConnection httpURLConnection = url.openConnection();
        long startTime = System.currentTimeMillis();
        httpURLConnection.connect();
        byte [] body = StreamUtils.copyToByteArray(httpURLConnection.getInputStream());
        long endTime = System.currentTimeMillis();
        logger.info("{} byte(s) received from {}. Time taken {} ms", body.length, url.getHost(), (endTime-startTime));
        return new String(body);
    }

}
