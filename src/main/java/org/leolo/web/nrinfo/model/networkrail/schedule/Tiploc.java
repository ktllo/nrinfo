package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Tiploc {
    private String tiplocCode;
    private String nalco;
    private String stanox;
    private String crsCode;
    private String description;
    private String tpsDescription;


    public Tiploc() {
    }

    public Tiploc(String tiplocCode, String nalco, String stanox, String crsCode, String description, String tpsDescription) {
        this.tiplocCode = tiplocCode;
        this.nalco = nalco;
        this.stanox = stanox;
        this.crsCode = crsCode;
        this.description = description;
        this.tpsDescription = tpsDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tiploc tiploc)) return false;
        return Objects.equals(tiplocCode, tiploc.tiplocCode) && Objects.equals(nalco, tiploc.nalco) && Objects.equals(stanox, tiploc.stanox) && Objects.equals(crsCode, tiploc.crsCode) && Objects.equals(description, tiploc.description) && Objects.equals(tpsDescription, tiploc.tpsDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tiplocCode, nalco, stanox, crsCode, description, tpsDescription);
    }
}
