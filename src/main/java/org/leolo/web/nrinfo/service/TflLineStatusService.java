package org.leolo.web.nrinfo.service;

import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

@Component
public class TflLineStatusService {

    private Logger logger = LoggerFactory.getLogger(TflLineStatusService.class);

    @Autowired
    private DataSource dataSource;

    @Getter
    private Date lastUpdate = null;

    private final Object LOCK = new Object();
//    private final Object

    @Autowired
    private TfLApiService tfLApiService;
//    @Autowired
//    private IrcService ircService;

    public void execute() throws IOException {
        logger.info("Job started");

    }
}
