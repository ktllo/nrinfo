package org.leolo.web.nrinfo.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Component
public class IrcConfigurationService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConfigurationService configurationService;

    private String host;

    private int port;

    private String nickname;

    private String ident;

    private boolean enabled;

    private String saslUsername;

    private String saslPassword;

    private String[] channels;

    private boolean tls;

    private String mainChannel;

    private String commandPrefix;

    private boolean init = false;

    public IrcConfigurationService() {
        if (configurationService == null) {
            log.error("configurationService is null!");
        } else {
            init();
        }

    }

    private synchronized void init() {
        if (!init) {
            //Init the value from database
            this.host = configurationService.getConfiguration("irc.host");
            this.port = Integer.parseInt(configurationService.getConfiguration("irc.port", "6667"));
            this.nickname = configurationService.getConfiguration("irc.nickname");
            this.ident = configurationService.getConfiguration("irc.ident");
            this.saslUsername = configurationService.getConfiguration("irc.sasl.username");
            this.saslPassword = configurationService.getConfiguration("irc.sasl.password");
            this.enabled = Boolean.parseBoolean(configurationService.getConfiguration("irc.enabled", "false"));
            this.tls = Boolean.parseBoolean(configurationService.getConfiguration("irc.tls", "false"));
            this.mainChannel = configurationService.getConfiguration("irc.mainChannel");
            this.commandPrefix = configurationService.getConfiguration("irc.commandPrefix", "!");
            this.channels = configurationService.getConfigurationGroupAll("irc.channel").toArray(new String[0]);
            init = true;
        }
    }

    public Collection<String> getChannels() {
        return Arrays.asList(channels);
    }

    public boolean hasSasl() {
        return saslPassword!=null && saslUsername!=null;
    }

    public boolean isEnabled() {
        if(!init){
            init();
        }
        return enabled;
    }

    public String getCommandPrefix() {
        if(!init){
            init();
        }
        return commandPrefix;
    }

    public String getMainChannel() {
        if(!init){
            init();
        }
        return mainChannel;
    }

    public boolean isTls() {
        if(!init){
            init();
        }
        return tls;
    }

    public String getSaslPassword() {
        if(!init){
            init();
        }
        return saslPassword;
    }

    public String getSaslUsername() {
        if(!init){
            init();
        }
        return saslUsername;
    }

    public String getIdent() {
        if(!init){
            init();
        }
        return ident;
    }

    public String getNickname() {
        if(!init){
            init();
        }
        return nickname;
    }

    public int getPort() {
        if(!init){
            init();
        }
        return port;
    }

    public String getHost() {
        if(!init){
            init();
        }
        return host;
    }
}
