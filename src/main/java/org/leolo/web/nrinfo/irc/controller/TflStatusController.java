package org.leolo.web.nrinfo.irc.controller;

import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.model.tfl.LineStatus;
import org.leolo.web.nrinfo.service.TflLineStatusService;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
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

    @Command(name="lustatus")
    public void getTflStatus(GenericMessageEvent event){
        String[] tokens = event.getMessage().split(" ");
        logger.debug("We have {} tokens", tokens.length);
        Map<String, List<LineStatus>> statusMap = tflLineStatusService.getLineStatusMap();
        if(tokens.length == 1){
            //Return simple responses
        } else  {
            //Return detailed responses

        }
    }

    @Command(name="ping")
    public void ping(GenericMessageEvent event){
        logger.info("ping called.");
        event.respond("pong");
    }
}
