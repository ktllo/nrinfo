package org.leolo.web.nrinfo.irc;

import lombok.Getter;
import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.irc.service.IrcUserService;
import org.leolo.web.nrinfo.service.IrcConfigurationService;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.cap.SASLCapHandler;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
public class IrcService extends ListenerAdapter {

    private final IrcUserService ircUserService;
    private Logger logger = LoggerFactory.getLogger(IrcService.class);
    @Getter
    private ApplicationContext applicationContext;
    private PircBotX bot;
    private IrcConfigurationService ircConfigurationService;
    private boolean initialized = false;
    private Hashtable<String, List<Method>> commandMap = new Hashtable<>();
    private TreeMap<String, String> aliasMap = new TreeMap<>();
    private long lastServerPing = 0;


    private synchronized void init() {
        if(!initialized) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);

            scanner.addIncludeFilter(new AnnotationTypeFilter(IrcController.class));

            for (BeanDefinition bd : scanner.findCandidateComponents("org.leolo.web.nrinfo")){
                try {
                    logger.debug("Irc Controller {} found", bd.getBeanClassName());
                    Class<? extends Object> clazz = Class.forName(bd.getBeanClassName());
                    IrcController ircController = clazz.getAnnotation(IrcController.class);
                    for (Method method : clazz.getDeclaredMethods()) {
                        logger.debug("----> method {}", method.getName());
                        if (method.isAnnotationPresent(Command.class)) {
                            Command cmd = method.getAnnotation(Command.class);
                            String cmdName = normalizeCommandName(cmd.value());
                            logger.debug("Irc Command {} found", cmdName);
                            List<Method> methods;
                            if (commandMap.containsKey(cmdName)) {
                                methods = commandMap.get(cmdName);
                            } else {
                                methods = new Vector<>();
                                commandMap.put(cmdName, methods);
                            }
                            methods.add(method);
                        }
                        if (ircController.init() && method.getName().equals("init") && method.getParameterCount() ==0 ){
                            Object handlerClass = applicationContext.getBean(method.getDeclaringClass());
                            try {
                                method.invoke(handlerClass);
                            } catch (IllegalAccessException|InvocationTargetException e) {
                                logger.error("Fail to initalize {} - {}", clazz.getName(), e.getMessage(), e);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("Cannot find class {}", bd.getBeanClassName());
                }
            }
            //Scan for duplication/incorrect method signature
            //Acceptable signature, by order of priority:
            // (Subclass of org.pircbotx.hooks.Event)
            // (org.pircbotx.hooks.Event),
            for(String command: commandMap.keySet()) {
                List<Method> methods = commandMap.get(command);
                logger.debug("Checking methods for command {}, {} methods registered", command, methods.size());
                List<Method> filteredMethods = new Vector<>();
                Method catchAll = null;
                for(Method method:methods) {
                    if (method.getParameterCount() == 1){
                        Parameter param = method.getParameters()[0];
                        if (param.getType().equals(Event.class) || param.getType().equals(GenericMessageEvent.class)){
                            if (catchAll==null) {
                                catchAll = method;
                            } else {
                                logger.warn("Duplicated method for {}.{}({}) handling {}",
                                        method.getDeclaringClass().getName(),
                                        method.getName(),
                                        Event.class.getName(),
                                        Event.class.getSimpleName());
                            }
                        } else if (Event.class.isAssignableFrom(method.getParameters()[0].getType())) {
                            //Need to scan for duplication
                            boolean ok = true;
                            for (Method addedMethod: filteredMethods) {
                                if(addedMethod.getParameters()[0].getType().equals(param.getType())) {
                                    ok = false;
                                    logger.warn("Duplicated method for {}.{}({}) handling {}",
                                            method.getDeclaringClass().getName(),
                                            method.getName(),
                                            param.getType().getName(),
                                            param.getType().getSimpleName());
                                    break;
                                }
                            }
                            if (ok) {
                                filteredMethods.add(method);
                            }
                        } else {
                            logger.warn("Unable to map {}.{}({}) as {} is not subclass of org.pircbotx.hooks.Event",
                                    method.getDeclaringClass().getName(),
                                    method.getName(),
                                    param.getType().getName(),
                                    param.getType().getName());
                        }
                    } else {
                        logger.info("{}.{} method has wrong number of parameter. Will be ignored", method.getDeclaringClass().getName(), method.getName());
                    }
                }
                if (catchAll != null){
                    filteredMethods.add(catchAll);
                }
                logger.info("Command {} has {} handler", command, filteredMethods.size());
                commandMap.put(command, filteredMethods);
            }

            initialized = true;
            for (String cmdName : commandMap.keySet()) {
                logger.info("Command {} found", cmdName);
            }
        }
    }

    private String normalizeCommandName(String commandName) {
        return commandName.toLowerCase().strip();
    }

    public IrcService(ApplicationContext context, IrcUserService ircUserService) {
        this.applicationContext = context;
        ircConfigurationService = context.getBean(IrcConfigurationService.class);
        init();
        this.ircUserService = ircUserService;
    }

    private boolean processCommand(String command, Event event){
        command = normalizeCommandName(command);
        logger.debug("Processing command {}", command);
        if (commandMap.containsKey(command)) {
            for (Method method:commandMap.get(command)) {
                logger.debug("Checking {}.{}({})",
                        method.getDeclaringClass().getName(),
                        method.getName(),
                        method.getParameters()[0].getType().getName()
                );
                if (method.getParameters()[0].getType().isAssignableFrom(event.getClass())) {
                    //We find a matching one
                    logger.debug("Dispatching to {}.{}({})",
                            method.getDeclaringClass().getName(),
                            method.getName(),
                            method.getParameters()[0].getType().getName()
                    );
                    try {
                        Object handlerClass = applicationContext.getBean(method.getDeclaringClass());
                        method.invoke(handlerClass, event);
                    } catch (Exception e) {
                        logger.error("Error when handling command : {}", e.getMessage(), e);
                        return false;
                    }
                    return true;
                }
            }
            logger.debug("No handler for {} with {}",
                    command,
                    event.getClass().getName()
            );
        }
        return false;
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        super.onMessage(event);
        if(event.getMessage().startsWith(ircConfigurationService.getCommandPrefix())) {
//            event = processAlias(event, true);
            String command = event.getMessage().split(" ")[0].substring(1);
            processCommand(command, event);
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
        super.onPrivateMessage(event);
        processCommand(event.getMessage().split(" ")[0], event);
    }

    @Override
    public void onNotice(NoticeEvent event) throws Exception {
        super.onNotice(event);
        processCommand(event.getMessage().split(" ")[0], event);
    }

    @Override
    public void onJoin(JoinEvent event) throws Exception {
        super.onJoin(event);
        logger.debug("User {} joined channel {}", event.getUser().getHostmask(), event.getChannel().getName());
        if (event.getUser().getNick().equalsIgnoreCase(event.getBot().getNick())){
            // It is us! Get the channel user list and check for everyone
        } else {
            ircUserService.checkHostmaskForLogin(event.getUserHostmask());
        }
    }

    @Override
    public void onUserList(UserListEvent event) throws Exception {
        super.onUserList(event);
        for(User user: event.getUsers()) {
            ircUserService.checkHostmaskForLogin(user.getHostmask());
        }
    }

    @Override
    public void onNickChange(NickChangeEvent event) throws Exception {
        super.onNickChange(event);
        logger.debug("Nickname change : {} -> {}", event.getOldNick(), event.getNewNick());
        ircUserService.nickChanges(event.getOldNick(), event.getNewNick());
    }

    @Override
    public void onPart(PartEvent event) throws Exception {
        super.onPart(event);
        logger.debug("User {} parted channel {}", event.getUser().getHostmask(), event.getChannel().getName());
        ircUserService.removeUser(event.getUserHostmask().getNick());
    }

    @Override
    public void onQuit(QuitEvent event) throws Exception {
        super.onQuit(event);
        logger.debug("User {} quitted", event.getUser().getHostmask());
        ircUserService.removeUser(event.getUserHostmask().getNick());
    }





    private <T extends GenericMessageEvent> T processAlias(T event, boolean hasPrefix) {
        String message = event.getMessage();
        return event;
    }

    @Override
    public void onPing(PingEvent event) throws Exception {
        super.onPing(event);
        logger.debug("SERVER PING - {}", event.getPingValue());
        lastServerPing = System.currentTimeMillis();
    }

    public void sendMessage(String message) {
        //TODO: use channel name in config
        if(bot!=null && bot.isConnected()) {
            if (bot.getState()== PircBotX.State.DISCONNECTED){
                return;
            }
            bot.sendIRC().message(ircConfigurationService.getMainChannel(), message);
        }
    }

    public synchronized void start() {
        if (!ircConfigurationService.isEnabled()) {
            logger.debug("IRC bot is disabled");
            return;
        }
        if (bot == null || !bot.isConnected()) {
            logger.info("Starting IRC bot service");
            //TODO: Get the parameter from configuration
            Configuration.Builder configuration = new Configuration.Builder()
                    .setAutoNickChange(true)
                    .setName(ircConfigurationService.getNickname())
                    .addServer(ircConfigurationService.getHost(), ircConfigurationService.getPort())
                    .setLogin(ircConfigurationService.getIdent())
                    .setAutoReconnect(true)
                    .addListener(this);
            if (ircConfigurationService.isTls()){
                configuration.setSocketFactory(SSLSocketFactory.getDefault());
            }
            configuration.addAutoJoinChannels(ircConfigurationService.getChannels());
            if (ircConfigurationService.hasSasl()){
                logger.debug("Login via SASL as {} with password {}",
                        ircConfigurationService.getSaslUsername(),
                        ircConfigurationService.getSaslPassword()
                );
                configuration.addCapHandler(new SASLCapHandler(ircConfigurationService.getSaslUsername(), ircConfigurationService.getSaslPassword()));
            }
            bot = new PircBotX(configuration.buildConfiguration());
            try {
                bot.startBot();
            } catch (IOException|IrcException e) {
                logger.error(e.getMessage(),e);
            }
        } else {
            logger.error("Bot already started.");
        }
    }

    public Collection<String> listCommand() {
        return commandMap.keySet();
    }

    public synchronized boolean registerAlias(String from, String to) {
        return false;
    }

}
