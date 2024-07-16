package org.leolo.web.nrinfo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Int;
import scala.Tuple2;
import scala.Tuple3;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PostcodeService {
    private Logger logger = LoggerFactory.getLogger(PostcodeService.class);

    @Autowired
    DataSource dataSource;

    public Tuple3<Integer, Integer, Integer> getPostcodeLocation(String postcode){
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT easting, northing, nvl(allowance, 25) as allowance FROM " +
                                "postcode p " +
                                "left outer join postcode_quality pq on p.qualtiy = pq.quality " +
                                "where postcode = ? and p.qualtiy <> 90"
                )
        ){
            pstmt.setString(1, postcode.replace(" ",""));
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()) {
                    return new Tuple3<>(
                            rs.getInt("easting"),
                            rs.getInt("northing"),
                            rs.getInt("allowance")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }
}
