package org.leolo.web.nrinfo.model.networkrail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SmartStepType {

    BETWEEN("B"),
    FROM("F"),
    TO("T"),
    INTERMEDIATE_FIRST("D"),
    CLEAROUT("C"),
    INTERPOSE("I"),
    INTERMEDIATE("E");

    private final String code;

    private SmartStepType(String code) {
        this.code = code;
    }

    @JsonCreator
    public static SmartStepType fromCode(String code) {
        for (SmartStepType type : SmartStepType.values()) {
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
