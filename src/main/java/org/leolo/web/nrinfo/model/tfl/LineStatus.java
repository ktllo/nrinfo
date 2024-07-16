package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class LineStatus {

    private int statusSeverity;
    private String statusSeverityDescription;
    private String reason;
    private String line;
    private String mode;

    private Date fromTime;
    private Date toTime;

    private List<String> stations = new Vector<>();
    private List<String> routes =  new Vector<>();

    public void addStation(String station) {
        stations.add(station);
    }

    public void addStations(Collection<String> stations) {
        this.stations.addAll(stations);
    }

    public void addStations(String... stations) {
        this.stations.addAll(Arrays.asList(stations));
    }

    public void addRoute(String line) {
        routes.add(line);
    }

    public void addRoutes(Collection<String> lines) {
        this.routes.addAll(lines);
    }

    public void addRoutes(String... lines) {
        this.routes.addAll(Arrays.asList(lines));
    }

    public int getStationCount(){
        return stations.size();
    }

    public int getRouteCount(){
        return routes.size();
    }

}
