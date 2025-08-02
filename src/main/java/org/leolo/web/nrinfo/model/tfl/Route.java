package org.leolo.web.nrinfo.model.tfl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Route {
    private UUID   routeId;
    private String lineId;
    private String direction;
    private String origin;
    private String destination;

    public Route(String lineId, String direction, String origin, String destination) {
        this.lineId = lineId;
        this.direction = direction;
        this.origin = origin;
        this.destination = destination;
    }

    public Route(UUID routeId, String lineId, String direction, String origin, String destination) {
        this.routeId = routeId;
        this.lineId = lineId;
        this.direction = direction;
        this.origin = origin;
        this.destination = destination;
    }
}
