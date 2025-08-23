package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class JsonScheduleV1Message {
    @JsonProperty("transaction_type")
    private TransactionType transactionType;

    @JsonProperty("schedule_start_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date scheduleStartDate;

    @JsonProperty("schedule_end_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date scheduleEndDate;

    @JsonProperty("schedule_days_runs")
    private String scheduleDaysRuns;

    @JsonProperty("CIF_bank_holiday_running")
    private String bankHolidayRunning;

    @JsonProperty("train_status")
    private String trainStatus;

    @JsonProperty("CIF_stp_indicator")
    private ScheduleIndicator stpIndicator;

    @JsonProperty("atoc_code")
    private String atocCode;

    @JsonProperty("applicable_timetable")
    private String applicableTimetable;

    @JsonProperty("schedule_segment")
    private ScheduleSegmentMessage scheduleSegment;

    @JsonProperty("CIF_train_uid") private String cifTrainUid;

    @JsonProperty("new_schedule_segment") private NewScheduleSegmentMessage newScheduleSegment;

}
