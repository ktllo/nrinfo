package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleSegmentMessage {

    @JsonProperty("CIF_train_category") private String trainCategory;
    @JsonProperty("signalling_id") private String signallingId;
    @JsonProperty("CIF_headcode") private String headcode;
    @JsonProperty("CIF_course_indicator") private String courseIndicator;
    @JsonProperty("CIF_train_service_code") private String serviceCode;
    @JsonProperty("CIF_business_sector") private String businessSector;
    @JsonProperty("CIF_power_type") private String powerType;
    @JsonProperty("CIF_timing_load") private String timingLoad;
    @JsonProperty("CIF_speed") private String speed;
    @JsonProperty("CIF_operating_characteristics") private String operatingCharacteristics;
    @JsonProperty("CIF_train_class") private String trainClass;
    @JsonProperty("CIF_sleepers") private String sleepers;
    @JsonProperty("CIF_reservations") private String reservations;
    @JsonProperty("CIF_connection_indicator") private String connectionIndicator;
    @JsonProperty("CIF_catering_code") private String cateringCode;
    @JsonProperty("CIF_service_branding") private String serviceBranding;

    @JsonProperty("schedule_location") private List<ScheduleLocationMessage> scheduleLocation;


}


