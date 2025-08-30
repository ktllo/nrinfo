package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionType {
    CREATE,
    UPDATE,
    DELETE;

    @JsonCreator
    public static TransactionType fromString(String str) {
        str = str.toLowerCase();
        if (str.equals("create")) {
            return CREATE;
        } else if (str.equals("update")) {
            return UPDATE;
        } else if (str.equals("delete")) {
            return DELETE;
        }
        return null;
    }
}
