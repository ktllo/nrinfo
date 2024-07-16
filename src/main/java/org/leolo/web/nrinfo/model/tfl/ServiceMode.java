package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ServiceMode {

    private String modeName;
    private boolean tflService;
    private boolean farePaying;
    private boolean scheduledService;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceMode that = (ServiceMode) o;

        if (tflService != that.tflService) return false;
        if (farePaying != that.farePaying) return false;
        if (scheduledService != that.scheduledService) return false;
        return Objects.equals(modeName, that.modeName);
    }

    @Override
    public int hashCode() {
        int result = modeName != null ? modeName.hashCode() : 0;
        result = 31 * result + (tflService ? 1 : 0);
        result = 31 * result + (farePaying ? 1 : 0);
        result = 31 * result + (scheduledService ? 1 : 0);
        return result;
    }

    public ServiceMode() {
    }

    public ServiceMode(String modeName, boolean tflService, boolean farePaying, boolean scheduledService) {
        this.modeName = modeName;
        this.tflService = tflService;
        this.farePaying = farePaying;
        this.scheduledService = scheduledService;
    }
}
