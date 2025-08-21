package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AssociationType {
    JOIN("JJ"),
    DIVIDE("VV"),
    NEXT("NP");
    private String code;

    public String getCode() {
        return code;
    }

    private AssociationType(String code) {
        this.code = code;
    }

    @JsonCreator
    public static AssociationType fromCode(String code) {
        for (AssociationType type : AssociationType.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
