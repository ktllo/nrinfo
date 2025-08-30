package org.leolo.web.nrinfo.service.networkrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.leolo.web.nrinfo.dao.networkrail.ScheduleAssociationDao;
import org.leolo.web.nrinfo.dao.networkrail.ScheduleDao;
import org.leolo.web.nrinfo.dao.networkrail.TiplocDao;
import org.leolo.web.nrinfo.model.networkrail.schedule.*;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;

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

    @Autowired private ScheduleDao scheduleDao;


    public void processDataLine(ArrayList<String> lines)  throws Exception {
        ArrayList<TiplocV1Message> tiplocs = new ArrayList<>();
        ArrayList<JsonAssociationV1Message> associations = new ArrayList<>();
        ArrayList<JsonScheduleV1Message> schedules = new ArrayList<>();
        for (String line : lines) {
            JsonNode node = mapper.readTree(line);
            String messageType = node.fieldNames().next();
            if (messageType.equals("JsonTimetableV1")) {
                logger.warn("JsonTimetableV1 is currently ignored");
            } else if (messageType.equals("TiplocV1")) {
                if ( "true".equalsIgnoreCase(configurationService.getConfiguration("dataload.networkrail.schedule.tiploc","false"))) {
                    tiplocs.add(mapper.convertValue(node.get("TiplocV1"), TiplocV1Message.class));
                }
            } else if (messageType.equals("JsonAssociationV1")) {
                if ( "true".equalsIgnoreCase(configurationService.getConfiguration("dataload.networkrail.schedule.association","false"))) {
                    associations.add(mapper.convertValue(node.get("JsonAssociationV1"), JsonAssociationV1Message.class));
                }
            } else if (messageType.equals("JsonScheduleV1")) {
                if ( "true".equalsIgnoreCase(configurationService.getConfiguration("dataload.networkrail.schedule.schedule","false"))) {
                    schedules.add(mapper.convertValue(node.get("JsonScheduleV1"), JsonScheduleV1Message.class));
                }
            } else if (messageType.equals("EOF")) {
                // Type EOF can be ignored
            } else {
                throw new RuntimeException("Message type " + messageType + " is not implemented");
            }
        }
        if (!tiplocs.isEmpty()) {
            processTiplocV1(tiplocs);
        }
        if (!associations.isEmpty()) {
            processJsonAssociationV1(associations);
        }
        if (!schedules.isEmpty()) {
            processJsonScheduleV1(schedules);
        }
    }

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

    private void processTiplocV1(ArrayList<TiplocV1Message> messages) throws Exception{
        ArrayList<Tiploc> insertOrUpdate = new ArrayList<>();
        ArrayList<Tiploc> delete = new ArrayList<>();
        for (TiplocV1Message message : messages) {
            if (message.getTransactionType() == TransactionType.CREATE || message.getTransactionType() == TransactionType.UPDATE) {
                insertOrUpdate.add(message.toTiploc());
            } else if (message.getTransactionType() == TransactionType.DELETE) {
                throw new RuntimeException("Not Implemented");
            }
        }
        tiplocDao.insertOrUpdate(insertOrUpdate);


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

    private void processJsonAssociationV1(ArrayList<JsonAssociationV1Message> messages) throws Exception{
        ArrayList<ScheduleAssociation> insertOrUpdate = new ArrayList<>();
        ArrayList<ScheduleAssociation> delete = new ArrayList<>();
        for (JsonAssociationV1Message message : messages) {
            if (message.getTransactionType() == TransactionType.CREATE || message.getTransactionType() == TransactionType.UPDATE) {
                insertOrUpdate.add(message.toScheduleAssociation());
            } else if (message.getTransactionType() == TransactionType.DELETE) {
                delete.add(message.toScheduleAssociation());
            }
        }
        if (!insertOrUpdate.isEmpty()) {
            scheduleAssociationDao.insertOrUpdateScheduleAssociation(insertOrUpdate);
        }
        if (!delete.isEmpty()) {
            scheduleAssociationDao.deleteAssociations(delete);
        }
    }

    private void processJsonScheduleV1(ArrayList<JsonScheduleV1Message> messages) throws Exception{
        ArrayList<Schedule> insert = new ArrayList<>();
        ArrayList<Schedule> update = new ArrayList<>();
        ArrayList<Schedule> delete = new ArrayList<>();
        for (JsonScheduleV1Message message : messages) {
            if (message.getTransactionType() == TransactionType.CREATE) {
                insert.add(message.toSchedule());
            } else if (message.getTransactionType() == TransactionType.UPDATE) {
                throw new RuntimeException("Not Implemented");
            } else if (message.getTransactionType() == TransactionType.DELETE) {
                delete.add(message.toSchedule());
            }
        }
        if (!insert.isEmpty()) {
            scheduleDao.insertSchedule(insert);
        }
        if (!delete.isEmpty()) {
            scheduleDao.deleteSchedules(delete);
        }
    }

}
