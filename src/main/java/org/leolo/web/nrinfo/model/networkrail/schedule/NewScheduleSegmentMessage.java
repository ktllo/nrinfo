package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewScheduleSegmentMessage {
    @JsonProperty("traction_class")
    private String tractionClass;
    @JsonProperty("uic_code")
    private String uicCode;
}
