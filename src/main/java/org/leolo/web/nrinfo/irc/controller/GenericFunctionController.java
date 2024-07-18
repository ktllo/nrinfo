package org.leolo.web.nrinfo.irc.controller;

import org.leolo.web.nrinfo.irc.IrcService;
import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.pircbotx.hooks.Event;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@IrcController
public class GenericFunctionController {

    @Autowired
    IrcService ircService;

    @Command("ping")
    public void ping(Event event){
        event.respond("pong");
    }

    @Command("list")
    public void getCommand(Event event) {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(ircService.listCommand());
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        sb.append("Available command : ");
        sb.append(String.join(", ", list));
        event.respond(sb.toString());
    }


}
