package org.leolo.web.nrinfo.irc.controller;

import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.irc.model.TflLineStatusAlert;
import org.leolo.web.nrinfo.irc.service.IrcUserService;
import org.leolo.web.nrinfo.service.UserService;
import org.pircbotx.Colors;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.text.SimpleDateFormat;
import java.util.*;

@IrcController(init = true)
@Qualifier("ircTflStatusController")
public class TflStatusController {

    private Logger logger = LoggerFactory.getLogger(TflStatusController.class);

    public static final String MISC_DATA_STATUS_ALERT = "tflStatusAlert.alertLevels";

}
