package org.leolo.web.nrinfo.job.model;

import lombok.Getter;
import lombok.Setter;
import org.leolo.web.nrinfo.job.JobStatus;

import java.util.UUID;

@Getter
@Setter
public class Job {
    private UUID jobUUID;
    private String jobName;
    private JobStatus status = JobStatus.RUNNING;

    public Job(UUID jobUUID, String jobName, JobStatus status) {
        this.jobUUID = jobUUID;
        this.jobName = jobName;
        this.status = status;
    }

    public Job(UUID jobUUID, String jobName) {
        this.jobUUID = jobUUID;
        this.jobName = jobName;
    }

    public Job() {
    }
}
