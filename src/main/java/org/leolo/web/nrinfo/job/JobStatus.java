package org.leolo.web.nrinfo.job;

public enum JobStatus {
    RUNNING("R"),
    COMPLETED("C"),
    FAILED("F");

    private String code;

    private JobStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static JobStatus fromCode(String code) {
        for (JobStatus jobStatus : JobStatus.values()) {
            if (jobStatus.code.equals(code)) {
                return jobStatus;
            }
        }
        return null;
    }
}
