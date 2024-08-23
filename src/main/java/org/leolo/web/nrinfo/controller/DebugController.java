package org.leolo.web.nrinfo.controller;

import org.leolo.web.nrinfo.irc.service.IrcUserService;
import org.leolo.web.nrinfo.service.TflLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @Autowired
    private TflLineService tflLineService;

    @Autowired
    private IrcUserService ircUserService;

    @RequestMapping("/debug/tflline")
    public Object listPartialMatchLineName(@RequestParam("q") String q){
        return tflLineService.getLineIdsByPartialLineName(q);
    }

    @RequestMapping("/debug/ircuser")
    public Object listIrcUser(){
        return ircUserService.getUserListSnapshot();
    }

}
