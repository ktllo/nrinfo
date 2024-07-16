package org.leolo.web.nrinfo.irc.controller;

import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.pircbotx.hooks.Event;

@IrcController
public class GenericFunctionController {
    @Command(name="ping")
    public void ping(Event event){
        event.respond("pong");
    }
}
