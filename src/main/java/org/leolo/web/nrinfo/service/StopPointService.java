package org.leolo.web.nrinfo.service;

import geotrellis.proj4.CRS;
import geotrellis.proj4.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Component
public class StopPointService {

    @Autowired
    private DataSource dataSource;

    private Logger logger = LoggerFactory.getLogger(StopPointService.class);

    public List<Object> findStopPointNear(int easting, int northing, int searchRange, boolean detailed){
        logger.debug("findStopPointNear({},{},{})", easting, northing, searchRange);
        List<Object> list = new Vector<>();
        try (Connection conn = dataSource.getConnection()){
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT " +
                            "atco_code, naptan_code, plate_code, common_name, short_common_name, " +
                            "landmark, street, crossing, `indicator`, bearing, " +
                            "nptg_code, locality_name, perent_locality_name, grandparent_locality_name, town, " +
                            "subrub, grid_type, easting, northing, " +
                            "longitude, latitude, stop_type, bus_stop_type, " +
                            "last_update_time, dist(easting, ?, northing, ?) AS distance " +
                            "FROM naptan_2025 " +
                            "WHERE " +
                            "    easting between ? and ? " +
                            "and northing between ? and ? " +
                            "and dist(easting, ?, northing, ?) <= ? " +
                            "order by dist(easting,?,northing,?) ASC"
            )){
                pstmt.setInt(1, easting);
                pstmt.setInt(2, northing);
                pstmt.setInt(3, easting-searchRange);
                pstmt.setInt(4, easting+searchRange);
                pstmt.setInt(5, northing-searchRange);
                pstmt.setInt(6, northing+searchRange);
                pstmt.setInt(7, easting);
                pstmt.setInt(8, northing);
                pstmt.setInt(9, searchRange);
                pstmt.setInt(10, easting);
                pstmt.setInt(11, northing);
                try(ResultSet rs = pstmt.executeQuery()){
                    while(rs.next()){
                        Map<String, Object> sp = new HashMap<>();
                        sp.put("atoc_code", rs.getString("atco_code"));
                        sp.put("common_name", rs.getString("common_name"));
                        sp.put("distance", rs.getString("distance"));
                        sp.put("easting", rs.getString("easting"));
                        sp.put("northing", rs.getString("northing"));
                        sp.put("longitude", rs.getDouble("longitude"));
                        sp.put("latitude", rs.getDouble("latitude"));
                        if(rs.wasNull()){
                            var toWgs84 = Transform.apply(CRS.fromEpsgCode(27700),CRS.fromEpsgCode(4326));
                            Tuple2<Object, Object> wgs84 = toWgs84.apply(rs.getDouble("easting"),rs.getDouble("northing"));
                            logger.debug("UKOS({},{})->WGS84({},{})",
                                    rs.getInt("easting"),rs.getInt("northing"),
                                    wgs84._1(), wgs84._2()
                            );
                            sp.put("longitude", wgs84._1());
                            sp.put("latitude", wgs84._2());
                        }
                        sp.put("stop_type", rs.getString("stop_type"));
                        // tan theta = delta e/delta n
                        int deltaE = easting -rs.getInt("easting");
                        int deltaN = northing - rs.getInt("northing");
                        logger.debug("delta E = {}; delta N = {}", deltaE, deltaN);
                        double rad = Math.atan2(deltaE, deltaN);
                        double degree = rad * 180 / Math.PI;
                        if(degree<180)
                            degree += 180;
                        logger.debug("Bearing {} deg", degree);
                        sp.put("bearing", Math.round(degree));
                        list.add(sp);
                    }
                }
            }
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
        return list;
    }

}
