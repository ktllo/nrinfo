package org.leolo.web.nrinfo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DataSource dataSource;

    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping("user/register")
    public Object register(
            @RequestParam String username,
            @RequestParam(required = false, defaultValue = "") String inviteKey,
            @RequestParam String password,
            @RequestParam String password2,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        logger.debug("username={}; inviteKey={}", username, inviteKey);
        Map<String, Object> result = new HashMap<String, Object>();
        long registerTime = System.currentTimeMillis();
        session.setAttribute(Constants.SESSION_USER_ID, null);
        boolean allowRegister = configurationService.getConfiguration(
                "user.allowRegister", "false").equalsIgnoreCase("true");
        if (!allowRegister && inviteKey.length() == 0) {
            logger.info("User from {} attempted to register with empty invite key", request.getRemoteAddr());
            result.put("result", "failure");
            result.put("message", "Invalid invite key");
            return result;
        }
        if (password.strip().length() == 0) {
            result.put("result", "failure");
            result.put("message", "Password required");
            return result;
        }
        if (!password.equals(password2)) {
            result.put("result", "failure");
            result.put("message", "Password mismatch");
            return result;
        }
        username = username.strip();
        try (Connection conn = dataSource.getConnection()) {
            //Check username
            if (!username.matches("^[a-zA-Z0-9]{4,100}$")) {
                result.put("result", "failure");
                result.put("message", "Invalid username name. Must between 4 and 100 characters, and being alphanumeric.");
                return result;
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM user WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result.put("result", "failure");
                        result.put("message", "User already exists.");
                        return result;
                    }
                }
            }
            if (inviteKey.length() > 0) {
                //Check key
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM user_invite_key " +
                                "WHERE invite_key = ? AND assigned_user is null and " +
                                "(expiry_date is null or expiry_date >= ?)"
                )) {
                    ps.setString(1, inviteKey);
                    ps.setTimestamp(2, new Timestamp(registerTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            result.put("result", "failure");
                            result.put("message", "Invalid invite key");
                            return result;
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user (username, created_date) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setString(1, username);
                ps.setTimestamp(2, new Timestamp(registerTime));
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    if (inviteKey.length() > 0) {
                        try (PreparedStatement psKeyUpd = conn.prepareStatement(
                                "UPDATE user_invite_key SET assigned_user=?, assigned_date=? WHERE invite_key=?"
                        )) {
                            psKeyUpd.setInt(1, id);
                            psKeyUpd.setTimestamp(2, new Timestamp(registerTime));
                            psKeyUpd.setString(3, inviteKey);
                            psKeyUpd.executeUpdate();
                        }
                    }
                    try (PreparedStatement psPwd = conn.prepareStatement(
                            "INSERT INTO user_password (user_id, password, password_date) VALUES (?,?,?)"
                    )) {
                        psPwd.setInt(1, id);
                        psPwd.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                        psPwd.setTimestamp(3, new Timestamp(registerTime));
                        psPwd.executeUpdate();
                    }
                    session.setAttribute(Constants.SESSION_USER_ID, Integer.toString(id));
                }
                result.put("result", "success");
            }
        } catch (SQLException sqlException) {
            result.put("result", "failure");
            result.put("message", "System Error");
            logger.error("Unable to create user - {}", sqlException.getMessage(), sqlException);
        }
        return result;
    }

    @RequestMapping("/logout")
    public Object logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", "success");
        return result;
    }


    public Object login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session){
        Map<String, Object> result = new HashMap<String, Object>();
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT u.user_id, up.password, up.password_date " +
                                "FROM user u join user_password up ON u.user_id = up.user_id " +
                                "WHERE u.username = ? AND u.status = 'A'"
                )
        ){
            boolean success = false;
            ps.setString(1, username);
            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    String dbPassword = rs.getString(2);
                    if (BCrypt.checkpw(password, dbPassword)){
                        success = true;
                    }
                } else {
                    //We hash the password anyway to stall a bit of time
                    BCrypt.hashpw(password, BCrypt.gensalt());
                }
            }
            if(success) {
                result.put("result", "success");
            } else {
                result.put("result", "failure");
                result.put("message", "Incorrect username or password");
            }
        }catch (SQLException e) {
            logger.error("Error when processing login - {}", e.getMessage(), e);
            result.put("result", "failure");
            result.put("message", "System Error");
        }
        return result;
    }
}
