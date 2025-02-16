package org.leolo.web.nrinfo.irc.controller;

import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.irc.IrcService;
import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.irc.service.IrcUserService;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.leolo.web.nrinfo.service.UserLinkKeyService;
import org.leolo.web.nrinfo.service.UserService;
import org.leolo.web.nrinfo.util.IrcUtil;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import scala.Tuple3;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Collection;

@IrcController(init = true)
public class IrcUserController {

    private Logger logger = LoggerFactory.getLogger(IrcUserController.class);
    private boolean initialized = false;

    @Autowired
    private ConfigurationService configService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private UserService userService;
    @Autowired
    private IrcUserService ircUserService;
    @Autowired
    private UserLinkKeyService userLinkKeyService;

    @Command("register")
    public void register(MessageEvent event){
        event.respond("This command is available via private message only");
        logger.debug("User is {}!{}@{}",
                event.getUserHostmask().getNick(),
                event.getUserHostmask().getIdent(),
                event.getUserHostmask().getHostname());
    }

    @Command("whoami")
    public void whoami(GenericMessageEvent event) {
        ircUserService.checkHostmaskForLogin(event.getUserHostmask());
        logger.debug("{} is asking who are they.", event.getUserHostmask().getNick());
        IrcUserService.UserEntry ue = ircUserService.getUserInfo(event.getUserHostmask().getNick());
        if (ue == null) {
            event.respond("I don't know who you are.");
        } else {
            event.respond(ue.getUserName());
        }
    }

    @Command("dykwia")
    public void dykwia(GenericMessageEvent event) {
        ircUserService.checkHostmaskForLogin(event.getUserHostmask());
        logger.debug("{} is asking do I know who they are.", event.getUserHostmask().getNick());
        if (ircUserService.getUserIdByNickname(event.getUserHostmask().getNick()) == IrcUserService.NO_USER_FOUND ){
            event.respond("No");
        } else {
            event.respond("Yes");
        }
    }

    @Command("identify")
    public void identify(PrivateMessageEvent event) {
        String [] tokens = event.getMessage().split(" ");
        String username;
        String password;
        if (tokens.length == 2) {
            //Password only
            username = event.getUserHostmask().getNick();
            password = tokens[1];
        } else if (tokens.length == 3) {
            //Username and password
            username = tokens[1];
            password = tokens[2];
        } else {
            event.respondWith("Format : identify [<username>] <password>");
            event.respondWith("<username> will be your nickname if not specified");
            return;
        }
        int userId = userService.login(username, password);
        if (userId == UserService.INVALID_USER) {
            event.respondWith("Incorrect username or password");
        } else {
            event.respondWith("Login success");
            logger.info("Logged in as uid_{}", userId);
            ircUserService.addUser(userId, event.getUserHostmask());
        }
    }

    @Command("hostmask")
    public void hostmask(GenericMessageEvent event){
        String [] tokens = event.getMessage().split(" ");
        ircUserService.checkHostmaskForLogin(event.getUserHostmask());
        if (tokens.length < 2) {
            hostmaskHelp(event);
            return;
        }
        //Check subcommands
        if ("list".equalsIgnoreCase(tokens[1])){
            hostmaskList(event);
        } else if ("add".equalsIgnoreCase(tokens[1])){
            addHostmask(event, tokens);
        } else if (
                "drop".equalsIgnoreCase(tokens[1]) ||
                        "remove".equalsIgnoreCase(tokens[1])
        ) {
            //We accept both drop and remove
            removeHostmask(event, tokens);
        }
    }

    @Command("link")
    public void incomingLink(GenericMessageEvent event) {
        String [] tokens = event.getMessage().split(" ");
        if (tokens.length < 2) {
            event.respondWith("Missing link key");
            return;
        }
        UserLinkKeyService.UserLinkResult ulr = userLinkKeyService.attemptLink(
                tokens[1] , "irc", true
        );
        if (ulr.isResult()){
            ircUserService.addUser(ulr.getUserId(), event.getUserHostmask());
            event.respondWith("Linked successfully!");
        } else {
            event.respondWith("Link failed.");
        }
    }

    private void addHostmask(GenericMessageEvent event, String[] tokens) {
        int userId = ircUserService.getUserIdByNickname(event.getUserHostmask().getNick());
        if (userId == IrcUserService.NO_USER_FOUND) {
            event.respond("Registered user only.");
            return;
        }
        if (tokens.length < 3){
            hostmaskHelp(event);
            return;
        }
        //Split
        String hostmask = tokens[2];
        try {
            Tuple3<String, String, String> splitMask = IrcUtil.splitHostmask(hostmask);
            String nickname = splitMask._1();
            String ident = splitMask._2();
            String host = splitMask._3();
            //Check duplicate
            if (userId == ircUserService.getUserIdByHostmask(nickname, ident, host)) {
                return;
            }
            ircUserService.addUserHostmask(
                    userId,
                    nickname,
                    ident,
                    host
            );
        } catch (RuntimeException e) {
            event.respond("Incorrect format");
        }
    }

    private void removeHostmask(GenericMessageEvent event, String[] tokens) {
        int userId = ircUserService.getUserIdByNickname(event.getUserHostmask().getNick());
        if (userId == IrcUserService.NO_USER_FOUND) {
            event.respond("Registered user only.");
            return;
        }
        if (tokens.length < 3){
            hostmaskHelp(event);
            return;
        }
        //Split
        String hostmask = tokens[2];
        try {
            Tuple3<String, String, String> splitMask = IrcUtil.splitHostmask(hostmask);
            String nickname = splitMask._1();
            String ident = splitMask._2();
            String host = splitMask._3();
            if (ircUserService.removeHostmask(
                    userId,
                    nickname,
                    ident,
                    host
            )){
                event.respondWith("Hostmask removed");
            } else {
                event.respondWith("Unable to remove hostmask");
            }

        } catch (RuntimeException e) {
            event.respond("Incorrect format");
        }
    }

    private void hostmaskList(GenericMessageEvent event) {
        int userId = ircUserService.getUserIdByNickname(event.getUserHostmask().getNick());
        if (userId == IrcUserService.NO_USER_FOUND) {
            event.respond("Registered user only.");
            return;
        }
        Collection<String> hostmaskList = ircUserService.listHostmask(userId);
        String list = String.join(", ", hostmaskList);
        event.respond(list);
    }

    private void hostmaskHelp(GenericMessageEvent event) {
    }

    @Command("register")
    public void register(PrivateMessageEvent event){
        //format : register <username> <password> [<inviteKey>]
        String tokens [] = event.getMessage().split(" ");
        if ((tokens.length==3 && configService.getConfiguration(
                "user.allowRegister", "false").equalsIgnoreCase("true"))|| tokens.length == 4) {
            //Length OK
            String username = tokens[1];
            String password = tokens[2];
            String inviteKey = tokens.length == 4 ? tokens[3] : null;
            UserService.UserCreatationResult userCreatationResult
                    = userService.createUser(username, password, inviteKey);
            if (userCreatationResult.isResult()){
                //Success
                //Save the hosmask
                //Add user to user map
                ircUserService.addUser(userCreatationResult.getUserId(), event.getUserHostmask());
            } else {
                event.respondWith(userCreatationResult.getMessage());
            }
        } else {
            logger.info("{} attempted to register with invalid paramter count {}", event.getUserHostmask().getHostmask(), tokens.length);
            registerHelp(event);
        }
    }

    private void registerHelp(GenericMessageEvent event){
        event.respondWith("/msg "+event.getBot().getNick()+" <username> <password> [<inviteKey>]");
    }


    public synchronized void init() {
        //Register alias
        if (initialized){
            return;
        }
        logger.info("Initialing UserController");
        initialized = true;
    }

}
