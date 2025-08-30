package org.leolo.web.nrinfo.model.networkrail.schedule;

import lombok.Getter;
import lombok.Setter;

import java.sql.Time;
import java.util.Objects;

@Getter
@Setter
public class ScheduleEntry {

    private String location;
    private int locationInstance = 1;

    private Time wttArrival;
    private Time wttPass;
    private Time wttDeparture;

    private Time gbttArrival;
    private Time gbttDeparture;

    private String platform;
    private String line;
    private String path;

    private Time engineeringAllowance;
    private Time pathingAllowance;
    private Time performanceAllowance;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ScheduleEntry that)) return false;
        return locationInstance == that.locationInstance && Objects.equals(location, that.location) && Objects.equals(wttArrival, that.wttArrival) && Objects.equals(wttPass, that.wttPass) && Objects.equals(wttDeparture, that.wttDeparture) && Objects.equals(gbttArrival, that.gbttArrival) && Objects.equals(gbttDeparture, that.gbttDeparture) && Objects.equals(platform, that.platform) && Objects.equals(line, that.line) && Objects.equals(path, that.path) && Objects.equals(engineeringAllowance, that.engineeringAllowance) && Objects.equals(pathingAllowance, that.pathingAllowance) && Objects.equals(performanceAllowance, that.performanceAllowance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, locationInstance, wttArrival, wttPass, wttDeparture, gbttArrival, gbttDeparture, platform, line, path, engineeringAllowance, pathingAllowance, performanceAllowance);
    }

    public void setLocationInstance(int locationInstance) {
        if (locationInstance <= 1)
            this.locationInstance = 1;
        else
            this.locationInstance = locationInstance;
    }
}
