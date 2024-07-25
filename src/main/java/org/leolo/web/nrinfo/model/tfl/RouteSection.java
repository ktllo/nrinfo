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
    @Setter
    private String routeDescription;

    @Getter
    private List<String> stopPoints = new Vector<>();

    public String getFrom() {
        if(stopPoints.size()==0)
            return null;
        return stopPoints.get(0);
    }

    public String getTo() {
        if(stopPoints.size()==0)
            return null;
        return stopPoints.get(stopPoints.size()-1);
    }


}
