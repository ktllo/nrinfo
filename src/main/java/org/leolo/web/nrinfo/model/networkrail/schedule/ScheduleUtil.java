package org.leolo.web.nrinfo.model.networkrail.schedule;

import java.sql.Time;

public class ScheduleUtil {

    public static Time parseTime(String time) {
        if (time == null || time.isEmpty()) {
            return null;
        }
        if (time.length() < 4) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }
        String hour = time.substring(0, 2);
        String minute = time.substring(2, 4);
        String second = time.length() >=5 && 'H' == time.charAt(4) ? "30": "00";

        return Time.valueOf(hour + ":" + minute + ":" + second);
    }

    public static Time parseAllowance(String time) {
        if (time == null || time.isEmpty()) {
            return null;
        }
        if (time.length() > 2) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }
        time = time.strip();
        if ("H".equals(time)) {
            return Time.valueOf("00:00:30");
        }
        if (time.endsWith("H")) {
            String minute = time.substring(0, 1);
            return Time.valueOf("00:" + minute+":30");
        }
        return Time.valueOf("00:"+time+":00");
    }

}
