package org.leolo.web.nrinfo.model.tfl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Line {
    private String lineId;
    private String lineName;
    private String modeCode;
}
