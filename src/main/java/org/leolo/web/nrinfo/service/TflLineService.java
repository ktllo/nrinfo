package org.leolo.web.nrinfo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

@Component
public class TflLineService {

    @Autowired
    private DataSource dataSource;

    private Logger logger = LoggerFactory.getLogger(TflLineService.class);

    public List<String> getLineIdsByPartialLineName(String name){
        Vector<String> lines = new Vector<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT line_id FROM tfl_line WHERE line_name LIKE ?"
                )
        ) {
            ps.setString(1, "%"+name+"%");
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    lines.add(rs.getString("line_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to find lines : {}", e.getMessage(), e);
        }
        return lines;
    }

    public String getLineNameById(String lineId) {
        try(
                Connection connection=dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                "SELECT line_name FROM tfl_line WHERE line_id = ?"
                )
        ){
            ps.setString(1, lineId);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    return rs.getString("line_name");
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to find lines : {}", e.getMessage(), e);
        }
        return null;
    }
}
