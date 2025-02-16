package org.leolo.web.nrinfo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UserLinkKeyService {

    private Map<String, UserLinkKeyEntry> keyMap = new Hashtable<>();
    private Logger logger = LoggerFactory.getLogger(UserLinkKeyService.class);
    public static final char [] KEY_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    public static final int DEFAULT_VALIDITY = 900_000;//900 seconds
    public static final int LINK_FAIL = -1;
    private Random random = new Random();
    private final Object SYNC_TOKEN = new Object();


    @Scheduled(fixedDelay = 60_000)
    public void purgeExpiredEntry(){
        Date current = new Date();
        List<String> removeSet = new ArrayList<>();
        logger.debug("Before purging : size = {}", keyMap.size());
        for (String key: keyMap.keySet()){
            if(keyMap.get(key).expired.before(current)){
                removeSet.add(key);
            }
        }
        if(removeSet.size()>0) {
            logger.info("Removing {} entries from user link keys", removeSet.size());
            for(String key:removeSet){
                keyMap.remove(key);
            }
        }
        logger.debug("After purging : size = {}", keyMap.size());
    }

    public String generateKey(int userId, String fromService, String toService) {
        return generateKey(userId, fromService, toService, DEFAULT_VALIDITY);
    }

    public String generateKey(int userId, String fromService, String toService, long validity) {
        UserLinkKeyEntry userLinkKeyEntry = new UserLinkKeyEntry();
        userLinkKeyEntry.fromService = fromService;
        userLinkKeyEntry.toService = toService;
        userLinkKeyEntry.userId = userId;
        userLinkKeyEntry.generated = new Date();
        userLinkKeyEntry.expired = new Date(userLinkKeyEntry.generated.getTime() + validity);
        String key;
        synchronized (SYNC_TOKEN){
            key = _generateKey();
            if(!keyMap.containsKey(key)){
                keyMap.put(key, userLinkKeyEntry);
            }
        }
        return key;
    }

    public UserLinkResult attemptLink (String linkKey, String toService) {
        return attemptLink(linkKey, toService, true);
    }

    public UserLinkResult attemptLink (String linkKey, String toService, boolean removeEntry) {
        UserLinkKeyEntry ulke = keyMap.get(linkKey);
        if (ulke == null || ulke.expired.before(new Date())) {
            // Key does not exist or expired
            return new UserLinkResult(false);
        }
        if (!(toService == null || ulke.toService==null) && ulke.toService.equalsIgnoreCase(toService)) {
            logger.info("User {} attempted to link with {} but the key is only valid with {}",
                    ulke.userId, toService, ulke.toService);
            return new UserLinkResult(false);
        }
        return new UserLinkResult(ulke.fromService, ulke.generated, ulke.userId, true);
    }

    private synchronized String _generateKey(){
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(KEY_CHARS[random.nextInt(KEY_CHARS.length)]);
        }
        return sb.toString();
    }

    private class UserLinkKeyEntry {
        String fromService;
        String toService;
        Date generated;
        Date expired;
        int userId;
    }

    public class UserLinkResult {
        private String fromService;
        private Date generated;
        private int userId;
        private boolean result;

        public String getFromService() {
            return fromService;
        }

        public Date getGenerated() {
            return generated;
        }

        public int getUserId() {
            return userId;
        }

        public boolean isResult() {
            return result;
        }

        public UserLinkResult(boolean result) {
            this.result = result;
        }

        public UserLinkResult(String fromService, Date generated, int userId, boolean result) {
            this.fromService = fromService;
            this.generated = generated;
            this.userId = userId;
            this.result = result;
        }
    }
}
