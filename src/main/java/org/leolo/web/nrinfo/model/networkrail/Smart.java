package org.leolo.web.nrinfo.model.networkrail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Smart {
    private String trainDescriberArea;
    private String fromBerth;
    private String toBerth;
    private String fromLine;
    private String toLine;
    private int berthOffset;
    private String platform;
    private SmartEventType eventType;
    private String route;
    private String stanoxCode;
    private String stationName;
    private SmartStepType stepType;
    private String comment;

    @JsonCreator
    public Smart(
            @JsonProperty("TD") String trainDescriberArea,
            @JsonProperty("FROMBERTH") String fromBerth,
            @JsonProperty("TOBERTH") String toBerth,
            @JsonProperty("FROMLINE") String fromLine,
            @JsonProperty("TOLINE") String toLine,
            @JsonProperty("BERTHOFFSET") int berthOffset,
            @JsonProperty("PLATFORM") String platform,
            @JsonProperty("EVENT") SmartEventType eventType,
            @JsonProperty("ROUTE") String route,
            @JsonProperty("STANOX") String stanoxCode,
            @JsonProperty("STANME") String stationName,
            @JsonProperty("STEPTYPE") SmartStepType stepType,
            @JsonProperty("COMMENT") String comment
    ) {
        this.trainDescriberArea = trainDescriberArea;
        this.fromBerth = fromBerth;
        this.toBerth = toBerth;
        this.fromLine = fromLine;
        this.toLine = toLine;
        this.berthOffset = berthOffset;
        this.platform = platform;
        this.eventType = eventType;
        this.route = route;
        this.stanoxCode = stanoxCode;
        this.stationName = stationName;
        this.stepType = stepType;
        this.comment = comment;
    }

    public Smart() {
        //Empty constructor
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Smart smart)) return false;
        return berthOffset == smart.berthOffset && Objects.equals(trainDescriberArea, smart.trainDescriberArea) && Objects.equals(fromBerth, smart.fromBerth) && Objects.equals(toBerth, smart.toBerth) && Objects.equals(fromLine, smart.fromLine) && Objects.equals(toLine, smart.toLine) && Objects.equals(platform, smart.platform) && eventType == smart.eventType && Objects.equals(route, smart.route) && Objects.equals(stanoxCode, smart.stanoxCode) && Objects.equals(stationName, smart.stationName) && stepType == smart.stepType && Objects.equals(comment, smart.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainDescriberArea, fromBerth, toBerth, fromLine, toLine, berthOffset, platform, eventType, route, stanoxCode, stationName, stepType, comment);
    }
}
