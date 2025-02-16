package org.leolo.web.nrinfo.model.tfl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class LineStatusTest {
    /*
    *  We are going to create different status
    *  statusA - base status
    *  statusB - identical case
    *  statusC - different start time
    *  statusD - different end time
    *  statusE - message is different
    */
    private static LineStatus statusA;
    private static LineStatus statusB;
    private static LineStatus statusC;
    private static LineStatus statusD;
    private static LineStatus statusE;

    @BeforeAll
    static void init() {
        // Base case
        statusA = new LineStatus();
        statusA.setLine("line");
        statusA.setStatusSeverity(1);
        statusA.setMode("mode");
        statusA.setReason("reason");
        statusA.setStatusSeverityDescription("severity");
        statusA.setFromTime(new Date(0));
        statusA.setToTime(new Date(1000));
        /*
         *  We are going to create different status
         *  statusA - base status
         *  statusB - identical case
         *  statusC - different start time
         *  statusD - different end time
         *  statusE - message is different
         */
        statusB = (LineStatus) statusA.clone();
        statusC = (LineStatus) statusA.clone();
        statusC.setFromTime(new Date(200));
        statusD = (LineStatus) statusA.clone();
        statusD.setToTime(new Date(100));
        statusE = (LineStatus) statusA.clone();
        statusE.setReason("anotherReason");
    }

    @Test
    void testEquals() {
        assertTrue(statusA.equals(statusB));
        assertFalse(statusA.equals(statusC));
        assertFalse(statusA.equals(statusD));
        assertFalse(statusA.equals(statusE));
    }
}