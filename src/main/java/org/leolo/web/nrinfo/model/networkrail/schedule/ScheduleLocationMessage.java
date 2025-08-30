package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleLocationMessage {

    @JsonProperty("record_identity") private ScheduleLocationType recordIdentity;
    @JsonProperty("tiploc_code") private String tiplocCode;
    @JsonProperty(value = "tiploc_instance", defaultValue = "0") private int tiplocInstance;
    @JsonProperty("arrival") private String arrival;
    @JsonProperty("departure") private String departure;
    @JsonProperty("pass") private String pass;
    @JsonProperty("public_arrival") private String publicArrival;
    @JsonProperty("public_departure") private String publicDeparture;
    @JsonProperty("platform") private String platform;
    @JsonProperty("line") private String line;
    @JsonProperty("path") private String path;
    @JsonProperty("engineering_allowance") private String engineeringAllowance;
    @JsonProperty("pathing_allowance") private String pathingAllowance;
    @JsonProperty("performance_allowance") private String performanceAllowance;
    @JsonProperty("location_type") private ScheduleLocationType locationType;


}
