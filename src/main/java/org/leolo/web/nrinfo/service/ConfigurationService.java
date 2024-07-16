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

@Component
public class ConfigurationService {

    private Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private DataSource dataSource;

    public String getConfiguration(String key, String defaultValue) {
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "select configuration_value from configuration where configuration_key = ?")
        ){
            stmt.setString(1, key);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    return rs.getString("configuration_value");
                }
                return defaultValue;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return defaultValue;
        }
    }

    public String getConfiguration(String key) {
        return getConfiguration(key, null);
    }

}
