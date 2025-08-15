package org.leolo.web.nrinfo.irc.model;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

public final class TflLineStatusAlert implements Serializable {

    public enum AlertLevel {
        NONE,
        STATUS_ONLY,
        STATUS_AND_PERIOD,
        ALL;
    }

    private Map<String, AlertLevel> alertLevelMap = new Hashtable<>();

    public void addAlert(String line, AlertLevel level){
        if (level != AlertLevel.NONE) {
            alertLevelMap.put(line, level);
        } else {
            alertLevelMap.remove(line);
        }
    }

    public void addAlert(String line) {
        addAlert(line, AlertLevel.STATUS_ONLY);
    }

    public AlertLevel getAlertLevel(String line) {
        AlertLevel level = alertLevelMap.get(line);
        return level == null ? AlertLevel.NONE : level;
    }

}
