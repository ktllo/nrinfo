package org.leolo.web.nrinfo.model.networkrail;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SmartEventType {

    ARRIVE_UP("A"),
    DEPART_UP("B"),
    ARRIVE_DOWN("C"),
    DEPART_DOWN("D");


    private final String code;

    private SmartEventType(String code) {
        this.code = code;
    }

    @JsonCreator
    public static SmartEventType fromCode(String code) {
        for (SmartEventType type : SmartEventType.values()) {
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
