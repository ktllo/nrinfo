package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.Vector;


public class Line {
    @Getter
    @Setter
    private String lineId;

    @Getter
    @Setter
    private String lineName;

    @Getter
    @Setter
    private ServiceMode mode;

    @Getter
    private List<RouteSection> routeSections = new Vector<>();
}
