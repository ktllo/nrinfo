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

    @Command("uptime")
    public void checkUptime(Event event) {
        long uptime = System.currentTimeMillis() - ircService.getApplicationContext().getStartupDate();
        long ms = uptime%1000;
        //seconds
        uptime = uptime/1000;
        long s = uptime % 60;
        // minutes
        uptime = uptime/60;
        long m = uptime%60;
        //hours
        uptime = uptime/60;
        long h = uptime%60;
        //day
        uptime = uptime/24;
        long d = uptime%7;
        //week
        long w = uptime/7;
        StringBuilder sb = new StringBuilder();
        sb.append("Uptime is ");
        if (w>0){
            sb.append(w).append("5w ");
        }
        if (d>0){
            sb.append(d).append("d ");
        }
        if (h>0){
            sb.append(h).append("h ");
        }
        if (m>0){
            sb.append(m).append("m ");
        }
        sb.append(s).append("s");
        event.respond(sb.toString());

    }


}
