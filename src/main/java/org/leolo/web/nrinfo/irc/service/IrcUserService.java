package org.leolo.web.nrinfo.irc.service;

import org.leolo.web.nrinfo.util.IrcUtil;
import org.pircbotx.UserHostmask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import scala.Tuple3;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class IrcUserService {

    public static final int NO_USER_FOUND = -1;

    public static final Locale SWEDISH = new Locale("sv");

    private static final Logger log = LoggerFactory.getLogger(IrcUserService.class);
    private final Object HOSTMASK_SYNC_TOKEN = new Object();

    private HashMap<String, UserEntry> userMap = new HashMap<>();
    private Vector<HostmaskEntry> maskEntries = new Vector<>();



    @Autowired
    private DataSource dataSource;

    public void addUser(int userId, UserHostmask userHostmask) {
        addUser(userId, userHostmask.getNick());
    }

    public void addUser(int userId, String nickname){
        UserEntry ue = new UserEntry(userId);
        fillUserEntry(ue);
        userMap.put(normalizeNickname(nickname), ue);
    }

    private void fillUserEntry(UserEntry ue) {
        try(Connection conn = dataSource.getConnection()){
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user.username FROM user WHERE user_id = ?"
            )) {
                ps.setInt(1, ue.userId);
                try(ResultSet rs = ps.executeQuery()){
                    if(rs.next()) {
                        ue.userName = rs.getString(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Unable to get user info - {} ", e.getMessage(), e);
        }
    }

    public void removeUser(String nickname) {
        nickname = normalizeNickname(nickname);
        userMap.remove(nickname);
    }

    public int getUserIdByNickname(String nickname) {
        nickname = normalizeNickname(nickname);
        if (userMap.containsKey(nickname)) {
            return userMap.get(nickname).userId;
        }
        return NO_USER_FOUND;
    }

    public void nickChanges(String oldNick, String newNick) {
        oldNick = normalizeNickname(oldNick);
        newNick = normalizeNickname(newNick);
        if (userMap.containsKey(oldNick)) {
            userMap.put(newNick, userMap.remove(oldNick));
        }
    }

    private String normalizeNickname(String nickname) {
        return nickname.toLowerCase(SWEDISH);
    }

    public UserEntry getUserInfo(String nickname) {
        nickname = normalizeNickname(nickname);
        if (userMap.containsKey(nickname)){
            return userMap.get(nickname);
        } else {
            return null;
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void updateHostmaskCache(){
        try(Connection conn = dataSource.getConnection()){
            Vector<HostmaskEntry> newMaskEntry = new Vector<>();
            try (
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT " +
                            "user_id, nickname, ident, host " +
                            "FROM user_irc_mask " +
                            "ORDER BY irc_mask_id")
            ) {
                while(rs.next()){
                    newMaskEntry.add(new HostmaskEntry(
                            rs.getInt(1),
                            normalizeNickname(rs.getString(2)),
                            normalizeNickname(rs.getString(3)),
                            rs.getString(4)
                    ));
                }
            }
            log.info("Updating hostmask cache, {} -> {}", maskEntries.size(), newMaskEntry.size());
            synchronized (HOSTMASK_SYNC_TOKEN) {
                maskEntries = newMaskEntry;
            }
        } catch (SQLException e) {
            log.error("Unable to update cache - {}", e.getMessage(), e);
        }
    }

    private String preprocessMaskEntry (String entry) {
        return entry.replace("*",".*").replace("?",".");
    }

    public Collection<String> listHostmask(int userId) {
        Vector<String> list = new Vector<>();
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                "SELECT nickname, ident, host from user_irc_mask where user_id = ? order by irc_mask_id"
                )
        ) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()){
                    list.add(String.format("%s!%s@%s", rs.getString(1), rs.getString(2), rs.getString(3)));
                }
            }
        } catch (SQLException e) {
            log.error("Error when getting list of hostmask - {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean addUserHostmask(int userId, String nickname, String ident, String host) {
        try(Connection conn = dataSource.getConnection()) {
            try(PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO user_irc_mask (user_id, nickname, ident, host) VALUES (?,?,?,?)"
            )) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, nickname);
                pstmt.setString(3, ident);
                pstmt.setString(4, host);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            log.error("Unable to add hostmask to user {} - {}", userId, e.getMessage(), e);
        }
        return false;
    }

    public boolean removeHostmask(int userId, String nickname, String ident, String host) {
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM user_irc_mask " +
                                "WHERE user_id=? AND nickname=? AND ident=? AND host=?"
                )
        ) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, nickname);
            pstmt.setString(3, ident);
            pstmt.setString(4, host);
            int rowAffected = pstmt.executeUpdate();
            return rowAffected == 1;
        } catch (SQLException e) {
            log.error("Unable to remove hostmask from user {} - {}", userId, e.getMessage(), e);
        }
        return false;
    }

    public int getUserIdByHostmask(String nickname, String ident, String host){
        log.debug("Checking {}!{}@{}", nickname, ident, host);
        Vector<HostmaskEntry> cachedEntry;
        synchronized (HOSTMASK_SYNC_TOKEN) {
            cachedEntry = maskEntries;
        }
        for(HostmaskEntry he: cachedEntry){
            if (
                    normalizeNickname(nickname).matches(preprocessMaskEntry(he.nick)) &&
                            normalizeNickname(ident).matches(preprocessMaskEntry(he.ident)) &&
                            host.matches(preprocessMaskEntry(he.host))
            ) {
                log.info("Matched uid_{} for {}!{}@{}", he.userId, nickname, ident, host);
                return he.userId;
            }
        }
        return NO_USER_FOUND;
    }

    public void checkHostmaskForLogin(UserHostmask userHostmask) {
        if (userMap.containsKey(normalizeNickname(userHostmask.getNick()))) {
            // User already logged in
            return;
        }
        String nickname = normalizeNickname(userHostmask.getNick());
        String ident = normalizeNickname(userHostmask.getIdent());
        String host = userHostmask.getHostname();
        int userId = getUserIdByHostmask(userHostmask.getNick(), userHostmask.getIdent(), userHostmask.getHostname());
        if (userId != NO_USER_FOUND) {
            addUser(userId, userHostmask);
        }
    }

    public void checkHostmaskForLogin(String userHostmask) {
        Tuple3<String, String, String> splittedMask = IrcUtil.splitHostmask(userHostmask);
        if (userMap.containsKey(normalizeNickname(splittedMask._1()))) {
            // User already logged in
            return;
        }
        int userId = getUserIdByHostmask(
                splittedMask._1(), splittedMask._2(), splittedMask._3()
        );
        if (userId != NO_USER_FOUND) {
            addUser(userId, userHostmask);
        }
    }

    public Map<String, String> getUserListSnapshot() {
        TreeMap<String, String> map = new TreeMap<>();
        for (String nickname: userMap.keySet()) {
            map.put(nickname, userMap.get(nickname).userName);
        }
        return map;
    }

    public class UserEntry {
        private int userId;
        private String userName;
        private UserEntry(int userId) {
            this.userId = userId;
        }

        public int getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }
    }

    private class HostmaskEntry {
        int userId;
        String nick;
        String ident;
        String host;

        public HostmaskEntry(int userId, String nick, String ident, String host) {
            this.userId = userId;
            this.nick = nick;
            this.ident = ident;
            this.host = host;
        }
    }

}
