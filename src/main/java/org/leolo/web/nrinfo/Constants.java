package org.leolo.web.nrinfo;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Constants {
    public static final String JOB_ID_TFL_LINE_STATUS = "tfl.lineStatus";

    @Deprecated
    public static final String PROP_TFL_PRIMARY_API_KEY = "remote.tfl.primary_key";
    @Deprecated
    public static final String PROP_TFL_SECONDARY_API_KEY = "remote.tfl.secondary_key";

    public static final String SESSION_USER_ID = "userId";

    public static final Marker MARKER_EXTERNAL_REQ = MarkerFactory.getMarker("EXT_REQ");
}
