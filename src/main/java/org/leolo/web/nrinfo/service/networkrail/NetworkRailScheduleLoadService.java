package org.leolo.web.nrinfo.service.networkrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.leolo.web.nrinfo.dao.networkrail.ScheduleAssociationDao;
import org.leolo.web.nrinfo.dao.networkrail.TiplocDao;
import org.leolo.web.nrinfo.model.networkrail.schedule.JsonAssociationV1Message;
import org.leolo.web.nrinfo.model.networkrail.schedule.TiplocV1Message;
import org.leolo.web.nrinfo.model.networkrail.schedule.TransactionType;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class NetworkRailScheduleLoadService {

    private Logger logger = LoggerFactory.getLogger(NetworkRailScheduleLoadService.class);

    private ObjectMapper mapper = new ObjectMapper();


    @Autowired
    private DataSource dataSource;

    @Autowired
    private NetworkRailApiRequestService networkRailApiRequestService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private TiplocDao tiplocDao;

    @Autowired
    private ScheduleAssociationDao scheduleAssociationDao;

    public void processDataLine(String dataLine) throws Exception{
        JsonNode node = mapper.readTree(dataLine);
        if (!node.isObject()) {
            throw new Exception("Data is not a JSON object");
        }
        String messageType = node.fieldNames().next();
        if (messageType.equals("JsonTimetableV1")) {
            logger.warn("JsonTimetableV1 is currently ignored");
            return;
        } else if (messageType.equals("TiplocV1")) {
            if ( "true".equalsIgnoreCase(configurationService.getConfiguration("dataload.networkrail.schedule.tiploc","false"))) {
                TiplocV1Message tiploc = mapper.convertValue(node.get("TiplocV1"), TiplocV1Message.class);
                processTiplocV1(tiploc);
            }
            return;
        } else if (messageType.equals("JsonAssociationV1")) {
            if ( "true".equalsIgnoreCase(configurationService.getConfiguration("dataload.networkrail.schedule.association","false"))) {
                JsonAssociationV1Message message = mapper.convertValue(node.get("JsonAssociationV1"), JsonAssociationV1Message.class);
                processJsonAssociationV1(message);
            }
            return;
        }
        throw new RuntimeException("Message type "+messageType+" is not implemented");
    }
    //Part 2: Handle the request

    private void processTiplocV1(TiplocV1Message message) throws Exception{
        if(message.getTransactionType() == TransactionType.CREATE) {
            //We need to do insert or update because we might already have the data when we do a full refresh
            tiplocDao.insertOrUpdateTiploc(message.toTiploc());
        } else if (message.getTransactionType() == TransactionType.UPDATE) {
            tiplocDao.updateTiploc(message.toTiploc());
        } else if (message.getTransactionType() == TransactionType.DELETE) {
            tiplocDao.deleteTiploc(message.toTiploc());
        }
    }

    private void processJsonAssociationV1(JsonAssociationV1Message message) throws Exception{
        if (message.getTransactionType() == TransactionType.CREATE) {
            scheduleAssociationDao.insertOrUpdateScheduleAssociation(message.toScheduleAssociation());
        } else if (message.getTransactionType() == TransactionType.UPDATE) {
            scheduleAssociationDao.updateScheduleAssociation(message.toScheduleAssociation());
        } else if (message.getTransactionType() == TransactionType.DELETE) {
            throw new RuntimeException("Not Implemented");
        }
    }

}
