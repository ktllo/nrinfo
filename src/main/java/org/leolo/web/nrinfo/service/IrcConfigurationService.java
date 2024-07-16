package org.leolo.web.nrinfo.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Component
public class IrcConfigurationService {

    @Value("${irc.host:#{null}}")
    @Getter
    private String host;

    @Value("${irc.port:6667}")
    @Getter
    private int port;

    @Value("${irc.nickname:trainbot}")
    @Getter
    private String nickname;

    @Value("${irc.ident:trainbot}")
    @Getter
    private String ident;

    @Value("${irc.enabled:false}")
    @Getter
    private boolean enabled;

    @Value("${irc.sasl.username:#{null}}")
    @Getter
    private String saslUsername;

    @Value("${irc.sasl.password:#{null}}")
    @Getter
    private String saslPassword;



    @Value("${irc.channels}")
    private String[] channels;

    @Value("${irc.tls:false}")
    @Getter
    private boolean tls;

    @Value("${irc.mainChannel}")
    @Getter
    private String mainChannel;

    @Value("${irc.commandPrefix:!}")
    @Getter
    private String commandPrefix;

    public Collection<String> getChannels() {
        return Arrays.asList(channels);
    }

    public boolean hasSasl() {
        return saslPassword!=null && saslUsername!=null;
    }
}
