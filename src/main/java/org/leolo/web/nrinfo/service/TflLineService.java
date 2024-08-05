package org.leolo.web.nrinfo.service;

import org.hibernate.annotations.processing.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Component
public class TflLineService {

    @Autowired
    private DataSource dataSource;

    private Map<String, String> lineIdToModeNameMap = new Hashtable<>();

    private Logger logger = LoggerFactory.getLogger(TflLineService.class);

    public List<String> getLineIdsByPartialLineName(String name){
        Vector<String> lines = new Vector<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT line_id FROM tfl_line WHERE line_name LIKE ?"
                )
        ) {
            logger.debug("PARAM 1: {}", "%"+name.strip()+"%");
            ps.setString(1, "%"+name.strip()+"%");
            try(ResultSet rs = ps.executeQuery()){
                int count = 0;
                while(rs.next()){
                    lines.add(rs.getString("line_id"));
                    count++;
                }
                logger.debug("Searching line name {} ---> {} row", name, count);
            }
        } catch (SQLException e) {
            logger.error("Unable to find lines : {}", e.getMessage(), e);
        }
        return lines;
    }
    public List<String> getLineIdsByLineName(String name){
        Vector<String> lines = new Vector<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT line_id FROM tfl_line WHERE line_name = ?"
                )
        ) {
            ps.setString(1, name.strip());
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

    public String getCachedLineModeByLineId(String lineId) {
        if (lineIdToModeNameMap.containsKey(lineId)){
            return lineIdToModeNameMap.get(lineId);
        }
        try(
                Connection connection = dataSource.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(
                        "SELECT tfl_line.service_mode FROM tfl_line WHERE line_id = ?"
                );
        ){
            pstmt.setString(1, lineId);
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    String serviceMode = rs.getString(1);
                    lineIdToModeNameMap.put(lineId, serviceMode);
                    return serviceMode;
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to get line mode : {}", e.getMessage(), e);
        }
        return null;
    }
}
