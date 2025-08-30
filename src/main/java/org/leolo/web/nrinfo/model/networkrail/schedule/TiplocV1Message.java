package org.leolo.web.nrinfo.model.networkrail.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TiplocV1Message {
    private TransactionType transactionType;
    private String tiplocCode;
    private String nalco;
    private String stanox;
    private String crsCode;
    private String description;
    private String tpsDescription;

    @JsonCreator
    public TiplocV1Message(
        @JsonProperty("transaction_type") String transactionType,
        @JsonProperty("tiploc_code") String tiplocCode,
        @JsonProperty("nalco") String nalco,
        @JsonProperty("stanox") String stanox,
        @JsonProperty("crs_code") String crsCode,
        @JsonProperty("description") String description,
        @JsonProperty("tps_description") String tpsDescription
    ) {
        this.transactionType = TransactionType.fromString(transactionType);
        this.tiplocCode = tiplocCode;
        this.nalco = nalco;
        this.stanox = stanox;
        this.crsCode = crsCode;
        this.description = description;
        this.tpsDescription = tpsDescription;
    }

    @Override
    public String toString() {
        return "TiplocV1Message{" +
                "transactionType=" + transactionType +
                ", tiplocCode='" + tiplocCode + '\'' +
                ", nalco='" + nalco + '\'' +
                ", stanox='" + stanox + '\'' +
                ", crsCode='" + crsCode + '\'' +
                ", description='" + description + '\'' +
                ", tpsDescription='" + tpsDescription + '\'' +
                '}';
    }

    public Tiploc toTiploc() {
        Tiploc tiploc = new Tiploc();
        tiploc.setTiplocCode(tiplocCode);
        tiploc.setNalco(nalco);
        tiploc.setStanox(stanox);
        tiploc.setCrsCode(crsCode);
        tiploc.setDescription(description);
        tiploc.setTpsDescription(tpsDescription);
        return tiploc;
    }
}
