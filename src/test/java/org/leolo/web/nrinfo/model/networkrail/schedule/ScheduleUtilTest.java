package org.leolo.web.nrinfo.model.networkrail.schedule;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Time;

public class ScheduleUtilTest {

    @Test
    public void testParsingTime() {
        Time time = ScheduleUtil.parseTime("2012");
        assertEquals(Time.valueOf("20:12:00"), time);
        time = ScheduleUtil.parseTime("2012H");
        assertEquals(Time.valueOf("20:12:30"), time);
        time = ScheduleUtil.parseTime("2012 ");
        assertEquals(Time.valueOf("20:12:00"), time);
    }

    @Test void testParsingAllowance() {
        Time time = null;
        time = ScheduleUtil.parseAllowance(" H");
        assertEquals(Time.valueOf("00:00:30"), time);
        time = ScheduleUtil.parseAllowance("H ");
        assertEquals(Time.valueOf("00:00:30"), time);
        time = ScheduleUtil.parseAllowance("1 ");
        assertEquals(Time.valueOf("00:01:00"), time);
        time = ScheduleUtil.parseAllowance(" 1");
        assertEquals(Time.valueOf("00:01:00"), time);
        time = ScheduleUtil.parseAllowance("1H");
        assertEquals(Time.valueOf("00:01:30"), time);
        time = ScheduleUtil.parseAllowance("15");
        assertEquals(Time.valueOf("00:15:00"), time);
    }
}
