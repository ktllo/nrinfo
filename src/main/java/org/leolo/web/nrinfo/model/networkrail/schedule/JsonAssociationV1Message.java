package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JsonAssociationV1Message {

    private TransactionType transactionType;
    private String mainTrainUID;
    private String assocTrainUID;
    private Date assocStartDate;
    private Date assocEndDate;
    private String assocDays;
    private AssociationType associationType;
    private String dateIndicator;
    private String location;
    private String baseLocationSuffix;
    private String assocLocationSuffix;
    private String diagramType;
    private ScheduleIndicator stpIndicator;

    public JsonAssociationV1Message(
            @JsonProperty("transaction_type") TransactionType transactionType,
            @JsonProperty("main_train_uid") String mainTrainUID,
            @JsonProperty("assoc_train_uid") String assocTrainUID,
            @JsonProperty(value = "assoc_start_date")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Date assocStartDate,
            @JsonProperty("assoc_end_date")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") Date assocEndDate,
            @JsonProperty("assoc_days") String assocDays,
            @JsonProperty("category") AssociationType associationType,
            @JsonProperty("date_indicator") String dateIndicator,
            @JsonProperty("location") String location,
            @JsonProperty("base_location_suffix") String baseLocationSuffix,
            @JsonProperty("assoc_location_suffix") String assocLocationSuffix,
            @JsonProperty("diagram_type") String diagramType,
            @JsonProperty("CIF_stp_indicator") ScheduleIndicator stpIndicator
    ) {
        this.transactionType = transactionType;
        this.mainTrainUID = mainTrainUID;
        this.assocTrainUID = assocTrainUID;
        this.assocStartDate = assocStartDate;
        this.assocEndDate = assocEndDate;
        this.assocDays = assocDays;
        this.associationType = associationType;
        this.dateIndicator = dateIndicator;
        this.location = location;
        this.baseLocationSuffix = baseLocationSuffix;
        this.assocLocationSuffix = assocLocationSuffix;
        this.diagramType = diagramType;
        this.stpIndicator = stpIndicator;
    }

    public ScheduleAssociation toScheduleAssociation() {
        ScheduleAssociation scheduleAssociation = new ScheduleAssociation();
        scheduleAssociation.setMainTrainUID(mainTrainUID);
        scheduleAssociation.setAssocTrainUID(assocTrainUID);
        scheduleAssociation.setAssocStartDate(assocStartDate);
        scheduleAssociation.setAssocEndDate(assocEndDate);
        scheduleAssociation.setAssocDays(assocDays);
        scheduleAssociation.setAssociationType(associationType);
        //dateInd
        switch(dateIndicator){
            case "N":
                scheduleAssociation.setDateIndicator(1);
                break;
            case "P":
                scheduleAssociation.setDateIndicator(-1);
                break;
            default:
                scheduleAssociation.setDateIndicator(0);
        }
        scheduleAssociation.setLocation(location);
        scheduleAssociation.setBaseLocationSuffix(baseLocationSuffix);
        scheduleAssociation.setAssocLocationSuffix(assocLocationSuffix);
        scheduleAssociation.setDiagramType(diagramType);
        scheduleAssociation.setStpIndicator(stpIndicator);
        return scheduleAssociation;
    }
}
