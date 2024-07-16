package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ServiceType {
    private UUID serviceTypeId = UUID.randomUUID();
    private String serviceTypeName;
}
