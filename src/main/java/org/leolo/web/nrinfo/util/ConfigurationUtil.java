package org.leolo.web.nrinfo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Hashtable;

@Component
@PropertySource("classpath:application.properties")
public class ConfigurationUtil {
    @Autowired
    private Environment env;

    private Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

    private Hashtable<String, String> cache = new Hashtable<>();

    public String getConfigValue(String configKey){
        if(cache.containsKey(configKey)){
            logger.debug("Cache hit for {}", configKey);
            return cache.get(configKey);
        }
        logger.debug("Cache misses for {}", configKey);
        String value = env.getProperty(configKey);
        cache.put(configKey, value);
        return  value;
    }

    public void invalidateCache(){
        cache.clear();
    }
}
