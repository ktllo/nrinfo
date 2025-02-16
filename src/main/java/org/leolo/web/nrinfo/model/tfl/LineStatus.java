package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class LineStatus implements Comparable<LineStatus>, Cloneable{

    private int statusSeverity;
    private String statusSeverityDescription;
    private String reason;
    private String line;
    private String mode;

    private Date fromTime;
    private EndTimeMode endTimeMode = EndTimeMode.UNKNOWN;
    private Date toTime;
    private long timeDiff;

    private List<String> stations = new Vector<>();
    private List<String> routes =  new Vector<>();


    public static Comparator<LineStatus> DEFAULT_COMPARATOR = new Comparator<LineStatus>() {
        @Override
        public int compare(LineStatus o1, LineStatus o2) {
            return o1.compareTo(o2);
        }
    };

    public static enum EndTimeMode {
        UNKNOWN,
        FIXED_TIME,
        FIXED_PERIOD;
    }

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

    public void setTimeDiff(long timeDiff) {
        long delta = timeDiff % 60_000;
        if (delta >= 30000) {
            this.timeDiff = timeDiff - delta + 60_000;
        } else {
            this.timeDiff = timeDiff - delta;
        }
    }

    @Override
    public int compareTo(LineStatus o) {
        int result = line.compareTo(o.line);
        if (result!=0) return result;
        result = Integer.compare(statusSeverity, o.statusSeverity);
        if (result!=0) return result;
        result = fromTime.compareTo(o.fromTime);
        if (result!=0) return result;
        result = toTime.compareTo(o.toTime);
        if (result!=0) return result;
        result = reason.compareTo(o.reason);
        if (result!=0) return result;
        result = statusSeverityDescription.compareTo(statusSeverityDescription);
        return result;
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (!(anotherObject instanceof LineStatus))
            return false;
        return compareTo((LineStatus) anotherObject) == 0;
    }

    @Override
    public Object clone() {
        LineStatus ls = new LineStatus();
        ls.statusSeverity = statusSeverity;
        ls.statusSeverityDescription = statusSeverityDescription;
        ls.reason = reason;
        ls.line = line;
        ls.mode = mode;
        ls.fromTime = new Date(fromTime.getTime());
        ls.toTime = new Date(toTime.getTime());
        ls.stations = List.copyOf(ls.stations);
        ls.routes = List.copyOf(ls.routes);
        return ls;
    }
}
