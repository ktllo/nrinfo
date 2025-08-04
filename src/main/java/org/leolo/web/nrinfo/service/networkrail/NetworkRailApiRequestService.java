package org.leolo.web.nrinfo.service.networkrail;

import org.leolo.web.nrinfo.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.GZIPInputStream;

@Component
public class NetworkRailApiRequestService {

    private Logger logger = LoggerFactory.getLogger(NetworkRailApiRequestService.class);

    @Autowired
    private ConfigurationService configurationService;

    public String sendRequest(String url) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                configurationService.getConfiguration("networkrail.username"),
                                configurationService.getConfiguration("networkrail.password").toCharArray()
                        );
                    }
                })
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (InterruptedException e) {
            logger.error("Request interrupted", e);
            throw new RuntimeException(e);
        }
    }
    public byte[] sendRequestBinary(String url, boolean preprocessData) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                configurationService.getConfiguration("networkrail.username"),
                                configurationService.getConfiguration("networkrail.password").toCharArray()
                        );
                    }
                })
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] bytes = response.body();
            if (preprocessData && bytes != null && bytes.length > 0) {
                if (bytes.length > 12 && bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b) {
                    int oldLength = bytes.length;
                    //Data is gzip
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    GZIPInputStream gis = new GZIPInputStream(bais);
                    byte[] buffer = new byte[1024];
                    int n;
                    while ((n = gis.read(buffer)) != -1) {
                        baos.write(buffer, 0, n);
                    }
                    gis.close();
                    bais.close();
                    baos.close();
                    bytes = baos.toByteArray();
                    logger.info("Data is gzipped. Received {} bytes. {} bytes after gunziped", oldLength, bytes.length);
                    return bytes;
                }
            }
            return bytes;
        } catch (InterruptedException e) {
            logger.error("Request interrupted", e);
            throw new RuntimeException(e);
        }
    }

    public byte[] sendRequestBinary(String url) throws IOException {
        return sendRequestBinary(url, true);
    }

}
