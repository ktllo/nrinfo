package org.leolo.web.nrinfo.dao.tfl;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import javax.naming.PartialResultException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Component
public class ServiceModeDao extends BaseDao {

    private Logger logger = LoggerFactory.getLogger(ServiceModeDao.class);

    @Autowired
    private DataSource dataSource;

    public List<ServiceMode> getAllServiceMode(){
        List<ServiceMode> serviceModes = new Vector<ServiceMode>();
        try(
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select * from tfl_mode")
        ){
            int processedCount = 0;
            while (rs.next()) {
                serviceModes.add(parseServiceMode(rs));
                processedCount++;
            }
            logger.info("Processed {} serviceModes", processedCount);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return serviceModes;
    }

    public ServiceMode getServiceModeByCode(String code){
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "select * from tfl_mode where mode_code = ?"
                )
        ){
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return parseServiceMode(rs);
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public Map<String, ServiceMode> getServiceModeMap() {
        Map<String, ServiceMode> map = new Hashtable<>();
        try(
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select * from tfl_mode")
        ){
            int processedCount = 0;
            while (rs.next()) {
                ServiceMode serviceMode = parseServiceMode(rs);
                map.put(serviceMode.getModeCode(), serviceMode);
                processedCount++;
            }
            logger.info("Processed {} serviceModes", processedCount);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return map;
    }

    private ServiceMode parseServiceMode(ResultSet rs) throws SQLException {
        ServiceMode serviceMode = new ServiceMode();
        serviceMode.setModeCode(rs.getString("mode_code"));
        serviceMode.setModeName(rs.getString("mode_name"));
        serviceMode.setTfl(parseBitAsBoolean(rs, "isTfl"));
        serviceMode.setHasStatusUpdate(parseBitAsBoolean(rs, "hasStatusUpdate"));
        serviceMode.setScheduled(parseBitAsBoolean(rs, "isScheduled"));
        serviceMode.setPaid(parseBitAsBoolean(rs, "isPaid"));
        return serviceMode;
    }


}
