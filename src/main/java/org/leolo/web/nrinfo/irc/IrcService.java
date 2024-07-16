package org.leolo.web.nrinfo.irc;

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
import java.util.Hashtable;
import java.util.Locale;

@Component
public class IrcService extends ListenerAdapter {

    private Logger logger = LoggerFactory.getLogger(IrcService.class);
    private ApplicationContext applicationContext;
    private PircBotX bot;
    private IrcConfigurationService ircConfigurationService;
    private boolean initialized = false;
    private Hashtable<String, Method> commandMap = new Hashtable<>();


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
                            String cmdName = cmd.value().toLowerCase();
                            logger.debug("Irc Command {} found", cmdName);
                            if (commandMap.containsKey(cmdName)) {
                                logger.warn("Duplicated command {}", cmdName);
                            } else {
                                commandMap.put(cmdName, method);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("Cannot find class {}", bd.getBeanClassName());
                }
            }
            initialized = true;
            for (String cmdName : commandMap.keySet()) {
                logger.info("Command {} found", cmdName);
            }
        }
    }

    public IrcService(ApplicationContext context) {
        this.applicationContext = context;
        ircConfigurationService = context.getBean(IrcConfigurationService.class);
        init();
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        super.onMessage(event);
        logger.debug("Message {} from {}", event.getMessage(), event.getChannel().getName());
        String message = event.getMessage();
        logger.debug("Command prefix is {}", ircConfigurationService.getCommandPrefix());
        if (message.startsWith(ircConfigurationService.getCommandPrefix())){
            String [] tokens = event.getMessage().split(" ");
            logger.debug("Token 0 is {}, checking {} as command", tokens[0],tokens[0].toLowerCase().substring(1) );
            if (commandMap.containsKey(tokens[0].toLowerCase().substring(1))) {
                Method method = commandMap.get(tokens[0].toLowerCase().substring(1));
                logger.debug("Command is handled by {}", method.getDeclaringClass().getName());
                Object controller;
                try {
                    controller = applicationContext.getBean(method.getDeclaringClass());
                    logger.info("Invoking method {}", method.getName());
                    method.invoke(controller, event);
                } catch (Exception e) {
                    logger.error("Error invoking method {}", method.getName(), e);
                }
            } else {
                logger.info("Command {} not found", tokens[0]);
            }
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
        super.onPrivateMessage(event);
        logger.debug("Message {} from {}", event.getMessage(), event.getUser().getNick());
        String [] tokens = event.getMessage().split(" ");
        logger.debug("Command may be {}", tokens[0].toLowerCase());
        if (commandMap.containsKey(tokens[0].toLowerCase())) {
            Method method = commandMap.get(tokens[0].toLowerCase());
            logger.debug("Command is handled by {}", method.getDeclaringClass().getName());
            Object controller;
            try {
                controller = applicationContext.getBean(method.getDeclaringClass());
                logger.info("Invoking method {}", method.getName());
                method.invoke(controller, event);
            } catch (Exception e) {
                logger.error("Error invoking method {}", method.getName(), e);
            }
        }
    }

    public void sendMessage(String message) {
        //TODO: use channel name in config
        if(bot!=null && bot.isConnected()) {
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



}
