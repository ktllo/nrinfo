package org.leolo.web.nrinfo.service;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

@Component
public class TflLineStatusService {

    private Logger logger = LoggerFactory.getLogger(TflLineStatusService.class);

    @Autowired
    private DataSource dataSource;

    private Map<String, List<LineStatus>> lineStatusMap = new HashMap<String, List<LineStatus>>();

    @Getter
    private Date lastUpdate = null;

    private final Object LOCK = new Object();
//    private final Object

    @Autowired
    private TfLApiService tfLApiService;
//    @Autowired
//    private IrcService ircService;

    public void execute() throws IOException {
        logger.info("Job started");
        //Step 1: Get the list of modes
        List<ServiceMode> modes = tfLApiService.getServiceMode();
        //Step 2: Fetch the status
        Map<String, List<LineStatus>> currentStatus = tfLApiService.getModeStatusByLine(
                modes.stream()
                        .filter((e)->e.isTflService())
                        .filter(e->!e.getModeName().equalsIgnoreCase("bus")) //We don't care about bus here
                        .toList());
        //TODO: Compare the map on another thread
        compareStatusMap(lineStatusMap, currentStatus);
        //Replacing the map
        synchronized (LOCK) {
            lineStatusMap = currentStatus;
            lastUpdate = new Date();
        }
    }

    private void compareStatusMap(
            final Map<String, List<LineStatus>> oldMap,
            final Map<String, List<LineStatus>> newMap
    ) {
        new Thread(()->{
            List<String> updateList = new ArrayList<>();
 mainLoop:  for(String lineId: newMap.keySet()) {
                //Step 1: find new line
                //Step 2: Compare line by line
                if (!oldMap.containsKey(lineId)) {
                    logger.debug("-->-->-->NEW LINE : {}", lineId);
                    //TODO: Update database
                    updateLineStatus(lineId, newMap.get(lineId));
                } else {
                    List<LineStatus> newStatuses = newMap.get(lineId);
                    List<LineStatus> oldStatuses = oldMap.get(lineId);
                    newStatuses.sort(LineStatus.DEFAULT_COMPARATOR);
                    oldStatuses.sort(LineStatus.DEFAULT_COMPARATOR);
                    //Check is the only status "Good Service"
                    if (newStatuses.size() == 1 && newStatuses.get(0).getStatusSeverity() == 10) {
                        //TODO: Update database
                        updateLineStatus(lineId, newStatuses);
                        if (oldStatuses.size() > 1 || (oldStatuses.size() == 1 && oldStatuses.get(0).getStatusSeverity() != 10)) {
                            updateList.add("There are Good Service now on " + tfLApiService.getLineName(lineId));
                        }
                        logger.debug("{}: -->--> GOOD SERVICE", lineId);
                        continue mainLoop;
                    }
                    Set<String> newLevel = new TreeSet<>();
                    Set<String> removedLevel = new TreeSet<>();
                    //Check for new status
                    for (LineStatus newStatus : newStatuses) {
                        boolean inOld = false;
                        for (LineStatus oldStatus : oldStatuses) {
                            if (newStatus.getStatusSeverity() == oldStatus.getStatusSeverity()) {
                                inOld = true;
                                break;
                            }
                        }
                        if (!inOld && newStatus.getStatusSeverity() != 10) {
                            newLevel.add(newStatus.getStatusSeverityDescription());
                        }
                    }
                    //Check for missing old status
                    for (LineStatus oldStatus : oldStatuses) {
                        boolean inNew = false;
                        for (LineStatus newStatus : newStatuses) {
                            if (newStatus.getStatusSeverity() == oldStatus.getStatusSeverity()) {
                                inNew = true;
                                break;
                            }
                        }
                        if (!inNew && oldStatus.getStatusSeverity() != 10) {
                            removedLevel.add(oldStatus.getStatusSeverityDescription());
                        }
                    }
                    boolean mapChanged = false;
                    if (oldStatuses.size() != newStatuses.size()) {
                        mapChanged = true;
                    } else {
                        for (int i = 0; i < oldStatuses.size(); i++) {
                            if (oldStatuses.get(i).compareTo(newStatuses.get(i)) != 0) {
                                mapChanged = true;
                                break;
                            }
                        }
                    }
                    if (mapChanged) {
                        updateLineStatus(lineId, newMap.get(lineId));
                    }
                }
            }
        }).start();
    }

    private synchronized void updateLineStatus(String lineId, List<LineStatus> lineStatuses) {
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement psHasLine = conn.prepareStatement(
                        "SELECT event_id FROM tfl_line_status WHERE line_id = ?"
                )
        ){
            conn.setAutoCommit(false);
            psHasLine.setString(1, lineId);
            boolean addedLine = false;
            String eventId = null;
            try(ResultSet rsHasLine = psHasLine.executeQuery()){
                if (!rsHasLine.next()) {
                    //Insert the base record
                    try (PreparedStatement psIns = conn.prepareStatement(
                            "INSERT INTO tfl_line_status (line_id, updated_date, event_id) VALUES (?, NOW(), NULL)"
                    )) {
                        psIns.setString(1, lineId);
                        int rowIns = psIns.executeUpdate();
                        logger.info("Inserted {} row(s) to tfl_line_status", rowIns);
                        addedLine = true;
                    }
                } else {
                    eventId = rsHasLine.getString(1);
                }
            }
            //Now we see are we handling a special case - Good Service
            if (lineStatuses.size()==1 && lineStatuses.get(0).getStatusSeverity()==10) {
                //Good Service!
                if (eventId != null) {
                    //Add a GOOD SERVICE history record
                    try(PreparedStatement psIns = conn.prepareStatement(
                            "INSERT INTO tfl_line_status_history " +
                                    "(event_id, history_version, line_id, status_message, from_time, to_time) " +
                                    "SELECT " +
                                    "   ?, MAX(history_version)+1, ?, NULL, NULL, NULL " +
                                    "FROM tfl_line_status_history " +
                                    "WHERE event_id = ?"
                    )) {
                        psIns.setString(1, eventId);
                        psIns.setString(2, lineId);
                        psIns.setString(3, eventId);
                        psIns.executeUpdate();
                    }
                    try (PreparedStatement psIns = conn.prepareStatement(
                            "INSERT INTO tfl_line_status_history_level (event_id, history_version, level_id) " +
                                    "SELECT ?, MAX(history_version),? FROM tfl_line_status_history WHERE event_id = ?"
                    )) {
                        psIns.setString(1, eventId);
                        psIns.setInt(2, lineStatuses.get(0).getStatusSeverity());
                        psIns.setString(3, eventId);
                        psIns.executeUpdate();
                    }
                }
                // Update the main records
                try (
                        PreparedStatement psMain = conn.prepareStatement(
                                "UPDATE tfl_line_status SET updated_date=NOW(), event_id = NULL where line_id = ?"
                        );
                        PreparedStatement psDel = conn.prepareStatement(
                                "DELETE FROM tfl_line_status_level WHERE line_id = ?"
                        );
                        PreparedStatement psLevels = conn.prepareStatement(
                                "INSERT INTO tfl_line_status_level (line_id, level_id) VALUES (?,?)"
                        )
                ) {
                    psMain.setString(1, lineId);
                    psDel.setString(1, lineId);
                    psLevels.setString(1, lineId);
                    psLevels.setInt(2, lineStatuses.get(0).getStatusSeverity());
                    psMain.executeUpdate();
                    psDel.executeUpdate();
                    psLevels.executeUpdate();
                }
            } else {
                StringBuilder message = new StringBuilder();
                Date from = null;
                Date to = null;
                for (LineStatus ls:lineStatuses) {
                    message.append(ls.getReason()).append("\r\n");
                    if (from == null || from.after(ls.getFromTime())){
                        from = ls.getFromTime();
                    }
                    if (to == null || to.before(ls.getToTime())) {
                        to = ls.getToTime();
                    }
                }
                if (eventId == null) {
                    eventId = DigestUtils.sha1Hex(lineId+"_"+System.currentTimeMillis());
                    logger.info("Generated event ID for {} : {}", lineId, eventId);
                    try(
                            PreparedStatement psM = conn.prepareStatement(
                                    "UPDATE tfl_line_status SET updated_date=NOW(), event_id=? WHERE line_id=?"
                            );
                            PreparedStatement psMDel = conn.prepareStatement(
                                    "DELETE FROM tfl_line_status_level WHERE line_id = ?"
                            );
                            PreparedStatement psMLevels = conn.prepareStatement(
                                    "INSERT INTO tfl_line_status_level (line_id, level_id) VALUES (?,?)"
                            );
                            PreparedStatement psH = conn.prepareStatement(
                                    "INSERT INTO tfl_line_status_history (event_id, history_version, line_id, status_message, from_time, to_time) " +
                                            "VALUES (?,1,?,?,?,?)"
                            );
                            PreparedStatement psHLevels = conn.prepareStatement(
                                    "INSERT INTO tfl_line_status_history_level (event_id, history_version, level_id) VALUES (?, 1, ?)"
                            )
                    ){
                        Set<Integer> existingLevels = new HashSet<>();
                        psM.setString(1, eventId);
                        psM.setString(2, lineId);
                        psMDel.setString(1, lineId);
                        psMLevels.setString(1, lineId);

                        psH.setString(1, eventId);
                        psH.setString(2, lineId);
                        psH.setString(3, message.toString());
                        psH.setTimestamp(4, new Timestamp(from.getTime()));
                        psH.setTimestamp(5, new Timestamp(to.getTime()));

                        psHLevels.setString(1, eventId);

                        for (LineStatus ls: lineStatuses) {
                            if (!existingLevels.contains(ls.getStatusSeverity())) {
                                psMLevels.setInt(2, ls.getStatusSeverity());
                                psHLevels.setInt(2, ls.getStatusSeverity());
                                psMLevels.addBatch();
                                psHLevels.addBatch();
                                existingLevels.add(ls.getStatusSeverity());
                            }
                        }

                        psM.executeUpdate();
                        psMDel.executeUpdate();
                        psMLevels.executeBatch();
                        psH.executeUpdate();
                        psHLevels.executeBatch();
                    }
                } else {
                    try(
                            PreparedStatement psM = conn.prepareStatement(
                                    "UPDATE tfl_line_status SET updated_date=NOW(), event_id=? WHERE line_id=?"
                            );
                            PreparedStatement psMDel = conn.prepareStatement(
                                    "DELETE FROM tfl_line_status_level WHERE line_id = ?"
                            );
                            PreparedStatement psMLevels = conn.prepareStatement(
                                    "INSERT INTO tfl_line_status_level (line_id, level_id) VALUES (?,?)"
                            );
                            PreparedStatement psH = conn.prepareStatement(
                                    "INSERT INTO tfl_line_status_history (event_id, history_version, line_id, status_message, from_time, to_time) " +
                                            "SELECT ?, MAX(history_version)+1,?,?,?,? FROM tfl_line_status_history WHERE event_id = ?"
                            );
                            PreparedStatement psHLevels = conn.prepareStatement(
                                    "INSERT INTO tfl_line_status_history_level (event_id, history_version, level_id) " +
                                            "SELECT ?, MAX(history_version),? FROM tfl_line_status_history WHERE event_id = ?"
                            )
                    ){
                        psM.setString(1, eventId);
                        psM.setString(2, lineId);
                        psMDel.setString(1, lineId);
                        psMLevels.setString(1, lineId);

                        psH.setString(1, eventId);
                        psH.setString(2, lineId);
                        psH.setString(3, message.toString());
                        psH.setTimestamp(4, new Timestamp(from.getTime()));
                        psH.setTimestamp(5, new Timestamp(to.getTime()));
                        psH.setString(6, eventId);

                        psHLevels.setString(1, eventId);
                        psHLevels.setString(3, eventId);

                        for (LineStatus ls: lineStatuses) {
                            psMLevels.setInt(2, ls.getStatusSeverity());
                            psHLevels.setInt(2, ls.getStatusSeverity());
                            psMLevels.addBatch();
                            psHLevels.addBatch();
                        }

                        psM.executeUpdate();
                        psMDel.executeUpdate();
                        psMLevels.executeBatch();
                        psH.executeUpdate();
                        psHLevels.executeBatch();
                    }
                }

            }
            conn.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public String getEventId(String lineId) {
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT event_id FROM tfl_line_status WHERE line_id = ?")
        ){
            pstmt.setString(1, lineId);
            try (ResultSet rs = pstmt.executeQuery()){
                if (rs.next()){
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to get event ID - {}", e.getMessage(), e);
        }
        return null;
    }

    public Map<String, List<LineStatus>> getLineStatusMap() {
        synchronized (LOCK) {
            return lineStatusMap;
        }
    }

}
