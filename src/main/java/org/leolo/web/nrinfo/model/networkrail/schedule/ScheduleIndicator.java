package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ScheduleIndicator {

    CANCELLATION("C"),
    NEW("N"),
    OVERLAY("O"),
    PERMANENT("P");
    private String code;

    private ScheduleIndicator(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ScheduleIndicator fromCode(String code) {
        for (ScheduleIndicator type : ScheduleIndicator.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
