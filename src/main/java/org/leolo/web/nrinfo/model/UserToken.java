package org.leolo.web.nrinfo.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class UserToken implements Serializable {

    @Getter
    @Setter
    private int userId;

}
