package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Vector;

public class RouteSection {

    @Getter
    @Setter
    private String serviceType;

    @Getter
    @Setter
    private String direction;

    @Getter
    @Setter
    private String lineString;

    @Getter
    private List<String> stopPoints = new Vector<>();
}
