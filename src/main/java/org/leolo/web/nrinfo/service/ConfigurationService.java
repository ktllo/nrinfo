package org.leolo.web.nrinfo.service;

import lombok.Getter;
import org.leolo.web.nrinfo.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class ConfigurationService {

    private Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private DataSource dataSource;

    private Map<String, CacheEntry> configCache = new Hashtable<>();

    public String getConfiguration(String key, String defaultValue) {
        //Check cache
        if (configCache.containsKey(key)) {
            CacheEntry cacheEntry = configCache.get(key);
            if (cacheEntry.expiry <= System.currentTimeMillis()) {
                //Cache hit
                return getCacheValue(cacheEntry, defaultValue);
            }
        }
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "select configuration_value, max_cache_time from configuration where configuration_key = ?")
        ){
            stmt.setString(1, key);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    String value = rs.getString("configuration_value");
                    CacheEntry cacheEntry = new CacheEntry();
                    cacheEntry.expiry = System.currentTimeMillis() + rs.getLong("max_cache_time");
                    cacheEntry.value = new String[1];
                    cacheEntry.value[0] = value;
                    configCache.put(key, cacheEntry);
                    return value;
                }
                return defaultValue;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return defaultValue;
        }
    }
    public String getConfigurationGroup(String key) {
        //Check cache
        if (configCache.containsKey(key)) {
            CacheEntry cacheEntry = configCache.get(key);
            if (cacheEntry.expiry <= System.currentTimeMillis()) {
                //Cache hit
                return getCacheValue(cacheEntry, null);
            }
        }
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "select configuration_key, configuration_value, max_cache_time " +
                                "from configuration where configuration_key like ?")
        ){
            stmt.setString(1, key+"%");
            try(ResultSet rs = stmt.executeQuery()){
                long cacheTime = Long.MAX_VALUE;
                ArrayList<String> datas = new ArrayList<>();
                while(rs.next()){
                    String value = rs.getString(2);
                    long thisCacheTime = rs.getLong(3);
                    if(cacheTime > thisCacheTime){
                        cacheTime = thisCacheTime;
                    }
                    CacheEntry cacheEntry = new CacheEntry();
                    cacheEntry.expiry = System.currentTimeMillis() + thisCacheTime;
                    cacheEntry.value = new String[1];
                    cacheEntry.value[0] = value;
                    configCache.put(rs.getString(1), cacheEntry);
                    datas.add(value);
                }
                String[] array = datas.toArray(new String[0]);
                CacheEntry cacheEntry = new CacheEntry();
                cacheEntry.expiry = System.currentTimeMillis() + cacheTime;
                cacheEntry.value = array;
                configCache.put(key, cacheEntry);
                return  getCacheValue(cacheEntry, null);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public Collection<String> getConfigurationGroupAll (String key) {
        if (configCache.containsKey(key)) {
            CacheEntry cacheEntry = configCache.get(key);
            if (cacheEntry.expiry <= System.currentTimeMillis()) {
                //Cache hit
                return Arrays.asList(cacheEntry.value);
            }
        }
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "select configuration_key, configuration_value, max_cache_time " +
                                "from configuration where configuration_key like ?")
        ){
            stmt.setString(1, key+"%");
            try(ResultSet rs = stmt.executeQuery()){
                long cacheTime = Long.MAX_VALUE;
                ArrayList<String> datas = new ArrayList<>();
                while(rs.next()){
                    String value = rs.getString(2);
                    long thisCacheTime = rs.getLong(3);
                    if(cacheTime > thisCacheTime){
                        cacheTime = thisCacheTime;
                    }
                    CacheEntry cacheEntry = new CacheEntry();
                    cacheEntry.expiry = System.currentTimeMillis() + thisCacheTime;
                    cacheEntry.value = new String[1];
                    cacheEntry.value[0] = value;
                    configCache.put(rs.getString(1), cacheEntry);
                    datas.add(value);
                }
                String[] array = datas.toArray(new String[0]);
                CacheEntry cacheEntry = new CacheEntry();
                cacheEntry.expiry = System.currentTimeMillis() + cacheTime;
                cacheEntry.value = array;
                configCache.put(key, cacheEntry);
                return Arrays.asList(cacheEntry.value);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private String getCacheValue(CacheEntry cacheEntry, String defaultValue) {
        if (cacheEntry.value == null) {
            log.warn("Cache value is null!");
            return defaultValue;
        }
        if (cacheEntry.value.length == 0) {
            log.warn("Cache value is empty!");
            return defaultValue;
        }
        if (cacheEntry.value.length == 1) {
            return cacheEntry.value[0];
        }
        return cacheEntry.value[CommonUtil.randomInt(0, cacheEntry.value.length)];
    }

    public String getConfiguration(String key) {
        return getConfiguration(key, null);
    }

    @Getter @Deprecated
    private final BigDecimal test = new BigDecimal(1);

    static class CacheEntry {
        long expiry;
        String[] value;

    }

}
