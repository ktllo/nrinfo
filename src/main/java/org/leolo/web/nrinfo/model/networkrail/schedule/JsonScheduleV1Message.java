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

    public Schedule toSchedule() {
        Schedule schedule = new Schedule();
        schedule.setTrainUID(cifTrainUid);
        schedule.setStartDate(scheduleStartDate);
        schedule.setEndDate(scheduleEndDate);
        schedule.setDaysRuns(scheduleDaysRuns);
        schedule.setRunsOnBankHolidays(bankHolidayRunning);
        schedule.setTrainStatus(trainStatus);
        schedule.setStpIndicator(stpIndicator);
        schedule.setAtocCode(atocCode);
        schedule.setSubjectToPerformanceMonitor("Y".equalsIgnoreCase(applicableTimetable));

        // Dig into schedule segment
        schedule.setTrainCategory(scheduleSegment.getTrainCategory());
        schedule.setSignallingHeadCode(scheduleSegment.getSignallingId());
        schedule.setReservationSystemHeadCode(scheduleSegment.getHeadcode());
        schedule.setTrainServiceCode(scheduleSegment.getServiceCode());
        //This field had been repurposed
        schedule.setPortionId(scheduleSegment.getBusinessSector());
        schedule.setPowerType(scheduleSegment.getPowerType());
        schedule.setTimingLoad(scheduleSegment.getTimingLoad());
        if (scheduleSegment.getSpeed() != null) {
            schedule.setPlannedSpeed(Integer.parseInt(scheduleSegment.getSpeed()));
        } else {
            schedule.setPlannedSpeed(0);
        }
        schedule.setOperatingCharacteristics(scheduleSegment.getOperatingCharacteristics());
        schedule.setHasFirstClass(!"S".equalsIgnoreCase(scheduleSegment.getTrainClass()));
        schedule.setSleeper(scheduleSegment.getSleepers());
        schedule.setReservations(scheduleSegment.getReservations());
        schedule.setCatering(scheduleSegment.getCateringCode());

        if (scheduleSegment.getScheduleLocation() != null) {
            //Now into each entry
            for (ScheduleLocationMessage slm : scheduleSegment.getScheduleLocation()) {
                ScheduleEntry se = new ScheduleEntry();
                se.setLocation(slm.getTiplocCode());
                se.setLocationInstance(slm.getTiplocInstance());
                se.setWttArrival(ScheduleUtil.parseTime(slm.getArrival()));
                se.setWttPass(ScheduleUtil.parseTime(slm.getPass()));
                se.setWttDeparture(ScheduleUtil.parseTime(slm.getDeparture()));
                se.setGbttArrival(ScheduleUtil.parseTime(slm.getPublicArrival()));
                se.setGbttDeparture(ScheduleUtil.parseTime(slm.getPublicDeparture()));
                se.setPlatform(slm.getPlatform());
                se.setLine(slm.getLine());
                se.setPath(slm.getPath());
                se.setEngineeringAllowance(ScheduleUtil.parseAllowance(slm.getEngineeringAllowance()));
                se.setPathingAllowance(ScheduleUtil.parseAllowance(slm.getPathingAllowance()));
                se.setPerformanceAllowance(ScheduleUtil.parseAllowance(slm.getPerformanceAllowance()));
                schedule.getScheduleLocations().add(se);
            }
        }

        return schedule;
    }

}
