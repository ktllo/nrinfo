package org.leolo.web.nrinfo.irc;

import lombok.Getter;
import org.leolo.web.nrinfo.irc.annotation.Command;
import org.leolo.web.nrinfo.irc.annotation.IrcController;
import org.leolo.web.nrinfo.service.IrcConfigurationService;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.cap.SASLCapHandler;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
public class IrcService extends ListenerAdapter {

    private Logger logger = LoggerFactory.getLogger(IrcService.class);
    @Getter
    private ApplicationContext applicationContext;
    private PircBotX bot;
    private IrcConfigurationService ircConfigurationService;
    private boolean initialized = false;
    private Hashtable<String, List<Method>> commandMap = new Hashtable<>();


    private synchronized void init() {
        if(!initialized) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);

            scanner.addIncludeFilter(new AnnotationTypeFilter(IrcController.class));

            for (BeanDefinition bd : scanner.findCandidateComponents("org.leolo.web.nrinfo")){
                try {
                    logger.debug("Irc Controller {} found", bd.getBeanClassName());
                    Class<? extends Object> clazz = Class.forName(bd.getBeanClassName());
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
                        if (param.getType().equals(Event.class)){
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

    public IrcService(ApplicationContext context) {
        this.applicationContext = context;
        ircConfigurationService = context.getBean(IrcConfigurationService.class);
        init();
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

}
