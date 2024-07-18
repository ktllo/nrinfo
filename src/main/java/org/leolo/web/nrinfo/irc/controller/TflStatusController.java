package org.leolo.web.nrinfo.irc.controller;

import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.service.TflLineStatusService;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@IrcController
public class TflStatusController {

    private Logger logger = LoggerFactory.getLogger(TflStatusController.class);

    @Autowired
    private TflLineStatusService tflLineStatusService;

    @Command("lustatus")
    public void getTflStatus(PrivateMessageEvent event){
        logger.debug("lustatus.private called");
    }

    @Command("lustatus")
    public void getTflStatus(MessageEvent event){
        logger.debug("lustatus.public called");
    }
}
