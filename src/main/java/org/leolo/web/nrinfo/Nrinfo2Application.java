package org.leolo.web.nrinfo;

import org.leolo.web.nrinfo.irc.IrcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Nrinfo2Application {

    public static void main(String[] args) {
        //Start the spring application
        ApplicationContext context = SpringApplication.run(Nrinfo2Application.class, args);
        IrcService ircService = context.getBean(IrcService.class);
        ircService.start();
    }

}
