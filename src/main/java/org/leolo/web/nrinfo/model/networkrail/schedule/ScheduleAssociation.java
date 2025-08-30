package org.leolo.web.nrinfo.model.networkrail.schedule;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Objects;

@Getter
@Setter
public class ScheduleAssociation {
    private String mainTrainUID;
    private String assocTrainUID;
    private Date assocStartDate;
    private Date assocEndDate;
    private String assocDays;
    private AssociationType associationType;
    private int dateIndicator;
    private String location;
    private String baseLocationSuffix;
    private String assocLocationSuffix;
    private String diagramType;
    private ScheduleIndicator stpIndicator;

    public ScheduleAssociation() {
    }

    public ScheduleAssociation(String mainTrainUID, String assocTrainUID, Date assocStartDate, Date assocEndDate, String assocDays, AssociationType associationType, int dateIndicator, String location, String baseLocationSuffix, String assocLocationSuffix, String diagramType, ScheduleIndicator stpIndicator) {
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ScheduleAssociation that)) return false;
        return Objects.equals(mainTrainUID, that.mainTrainUID) && Objects.equals(assocTrainUID, that.assocTrainUID) && Objects.equals(assocStartDate, that.assocStartDate) && Objects.equals(assocEndDate, that.assocEndDate) && Objects.equals(assocDays, that.assocDays) && associationType == that.associationType && Objects.equals(dateIndicator, that.dateIndicator) && Objects.equals(location, that.location) && Objects.equals(baseLocationSuffix, that.baseLocationSuffix) && Objects.equals(assocLocationSuffix, that.assocLocationSuffix) && Objects.equals(diagramType, that.diagramType) && stpIndicator == that.stpIndicator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainTrainUID, assocTrainUID, assocStartDate, assocEndDate, assocDays, associationType, dateIndicator, location, baseLocationSuffix, assocLocationSuffix, diagramType, stpIndicator);
    }

    @Override
    public String toString() {
        return "ScheduleAssociation{" +
                "mainTrainUID='" + mainTrainUID + '\'' +
                ", assocTrainUID='" + assocTrainUID + '\'' +
                ", assocStartDate=" + assocStartDate +
                ", assocEndDate=" + assocEndDate +
                ", assocDays='" + assocDays + '\'' +
                ", associationType=" + associationType +
                ", dateIndicator='" + dateIndicator + '\'' +
                ", location='" + location + '\'' +
                ", baseLocationSuffix='" + baseLocationSuffix + '\'' +
                ", assocLocationSuffix='" + assocLocationSuffix + '\'' +
                ", diagramType='" + diagramType + '\'' +
                ", stpIndicator=" + stpIndicator +
                '}';
    }
}
