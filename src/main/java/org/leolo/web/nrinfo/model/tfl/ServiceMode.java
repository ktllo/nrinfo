package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

/**
 * Model for table tflMode
 */
@Getter
@Setter
public class ServiceMode {

    private String modeCode;
    private String modeName;
    private boolean isTfl;
    private boolean hasStatusUpdate;
    private boolean scheduled;
    private boolean paid;

    public ServiceMode(String modeCode, String modeName, boolean isTfl, boolean hasStatusUpdate, boolean scheduled, boolean paid) {
        this.modeCode = modeCode;
        this.modeName = modeName;
        this.isTfl = isTfl;
        this.hasStatusUpdate = hasStatusUpdate;
        this.scheduled = scheduled;
        this.paid = paid;
    }

    public ServiceMode(){

    }
}
