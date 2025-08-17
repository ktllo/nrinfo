package org.leolo.web.nrinfo.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.leolo.web.nrinfo.model.UserToken;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Hashtable;
import java.util.Map;

@Service
public class UserTokenService {

    private Logger logger = LoggerFactory.getLogger(UserTokenService.class);

    private final Object SYNC_LOCK = new Object();

    private Map<String, UserToken> tokenStore = new Hashtable<>();

    @Autowired
    private ConfigurationService configurationService;

    public String generateBasicToken(int userId) {
        String token = null;
        int suffix = 0;
        while(true) {
            String rawToken = "BASIC:" +
                    userId + ':' +
                    System.currentTimeMillis() + ':' +
                    configurationService.getConfiguration("user.loginToken.secret", "") +
                    (suffix!=0?(":"+suffix):"");
            token = DigestUtils.sha3_384Hex(rawToken);
            synchronized (SYNC_LOCK) {
                if (!tokenStore.containsKey(token)) {
                    UserToken userToken = new UserToken();
                    userToken.setUserId(userId);
                    tokenStore.put(token, userToken);
                    break;
                }
            }
            if (++suffix < 5) {
                logger.debug("Duplicated token {}", token);
            } else {
                logger.warn("Duplicated token {}", token);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted - {}", e.getMessage(), e);
                }
            }
        }
        return token;
    }
}
