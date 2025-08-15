package org.leolo.web.nrinfo.model.networkrail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Corpus {

    private String stanox;
    private String uicCode;
    private String crsCode;
    private String tiploc;
    private String nlc;
    private String shortName;
    private String longName;

    @JsonCreator
    public Corpus(
            @JsonProperty("STANOX")    String stanox,
            @JsonProperty("UIC")       String uicCode,
            @JsonProperty("3ALPHA")    String crsCode,
            @JsonProperty("TIPLOC")    String tiploc,
            @JsonProperty("NLC")       String nlc,
            @JsonProperty("NLCDESC16") String shortName,
            @JsonProperty("NLCDESC")   String longName
    ) {
        this.stanox = stanox;
        this.uicCode = uicCode;
        this.crsCode = crsCode;
        this.tiploc = tiploc;
        this.nlc = nlc;
        this.shortName = shortName;
        this.longName = longName;
    }

    @Override
    public String toString() {
        return "Corpus{" +
                "stanox='" + stanox + '\'' +
                ", uicCode='" + uicCode + '\'' +
                ", crsCode='" + crsCode + '\'' +
                ", tiploc='" + tiploc + '\'' +
                ", nlc='" + nlc + '\'' +
                ", shortName='" + shortName + '\'' +
                ", longName='" + longName + '\'' +
                '}';
    }
}
