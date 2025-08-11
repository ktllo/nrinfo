package org.leolo.web.nrinfo.service.networkrail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.leolo.web.nrinfo.dao.networkrail.CorpusDao;
import org.leolo.web.nrinfo.dao.networkrail.SmartDao;
import org.leolo.web.nrinfo.model.networkrail.Corpus;
import org.leolo.web.nrinfo.model.networkrail.Smart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Component
public class NetworkRailDataLoadService {

    private Logger logger = LoggerFactory.getLogger(NetworkRailDataLoadService.class);

    private static final Object LOCK_STATIC_DATA = new Object();

    public static final String URL_CORPUS = "https://publicdatafeeds.networkrail.co.uk/ntrod/SupportingFileAuthenticate?type=CORPUS";
    public static final String URL_SMART = "https://publicdatafeeds.networkrail.co.uk/ntrod/SupportingFileAuthenticate?type=SMART";
    @Autowired
    private NetworkRailApiRequestService networkRailApiRequestService;

    @Autowired
    private CorpusDao corpusDao;

    @Autowired
    private SmartDao smartDao;

    public Method getLoaderMethod(String type) {
        try {
            if (type.equalsIgnoreCase("CORPUS")) {
                return NetworkRailDataLoadService.class.getDeclaredMethod("loadCORPUS", UUID.class);
            } else if (type.equalsIgnoreCase("SMART")) {
                return NetworkRailDataLoadService.class.getDeclaredMethod("loadSMART", UUID.class);
            }
        } catch (NoSuchMethodException e) {
            logger.error("Unable to find declared method - {}", e.getMessage(), e);
        }
        return null;
    }

    public void loadCORPUS(UUID jobUUID) {
        logger.info("Loading CORPUS data for job {}", jobUUID);
        synchronized (LOCK_STATIC_DATA) {
            //Download data
            logger.info("Downloading from {}", URL_CORPUS);
            byte[] data = null;
            try {
                data = networkRailApiRequestService.sendRequestBinary(URL_CORPUS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String corpusData = new String(data);
            logger.info("CORPUS data loaded. length = {}", data.length);
            //Parse data
            ObjectMapper mapper = new ObjectMapper();
            List<Corpus> corpusList = null;
            try {
                JsonNode rootNode = mapper.readTree(corpusData);
                JsonNode corpusNode = rootNode.get("TIPLOCDATA");
                corpusList = mapper.readValue(corpusNode.traverse(), new TypeReference<List<Corpus>>() {});
            } catch (IOException e) {
                logger.error("JSON Parsing error - {}",e.getMessage(), e);
                throw new RuntimeException(e);
            }
            logger.info("CORPUS data loaded. Number of rows = {}", corpusList.size());
            //Load into database
            corpusDao.addAll(corpusList);
        }
    }

    public void loadSMART(UUID jobUUID) {
        logger.info("Loading SMART data for job {}", jobUUID);
        synchronized (LOCK_STATIC_DATA) {
            //Download data
            logger.info("Downloading from {}", URL_SMART);
            byte[] data = null;
            try {
                data = networkRailApiRequestService.sendRequestBinary(URL_SMART);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String corpusData = new String(data);
            logger.info("SMART data loaded. length = {}", data.length);
            //Parse data
            ObjectMapper mapper = new ObjectMapper();
            List<Smart> smartList = null;
            try {
                JsonNode rootNode = mapper.readTree(corpusData);
                JsonNode corpusNode = rootNode.get("BERTHDATA");
                smartList = mapper.readValue(corpusNode.traverse(), new TypeReference<List<Smart>>() {});
            } catch (IOException e) {
                logger.error("JSON Parsing error - {}",e.getMessage(), e);
                throw new RuntimeException(e);
            }
            logger.info("SMART data loaded. Number of rows = {}", smartList.size());
            //Load into database
            smartDao.truncateTable();
            smartDao.addAll(smartList);
        }
    }
}
