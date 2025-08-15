package org.leolo.web.nrinfo.controller;

import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.leolo.web.nrinfo.util.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    public Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    ConfigurationUtil configurationUtil;

    @Autowired
    ConfigurationService configurationService;

    @RequestMapping(path = "/api/test")
    public void test(){
        logger.info("Is null? {}", configurationUtil==null);
        logger.info("Key = {}",configurationUtil.getConfigValue(Constants.PROP_TFL_PRIMARY_API_KEY) );
        logger.info("test = {}", configurationService.getTest() );
    }
}
