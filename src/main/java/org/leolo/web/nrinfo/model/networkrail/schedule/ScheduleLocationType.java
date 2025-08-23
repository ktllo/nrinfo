package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ScheduleLocationType {

    ORIGINATE("LO"),
    INTERMEDIATE("LI"),
    TERMINATE("LT");



    private String code;

    ScheduleLocationType(String code) {
        this.code = code;
    }

    @JsonCreator
    public static ScheduleLocationType fromCode(String code) {
        for (ScheduleLocationType type : ScheduleLocationType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
