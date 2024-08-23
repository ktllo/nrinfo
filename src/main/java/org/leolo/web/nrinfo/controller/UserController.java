package org.leolo.web.nrinfo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.leolo.web.nrinfo.Constants;
import org.leolo.web.nrinfo.service.ConfigurationService;
import org.leolo.web.nrinfo.service.UserService;
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

    @Autowired
    private UserService userService;

    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping("user/register")
    public Object register(
            @RequestParam String username,
            @RequestParam(required = false, defaultValue = "") String inviteKey,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        logger.debug("username={}; inviteKey={}", username, inviteKey);
        Map<String, Object> result = new HashMap<String, Object>();
        session.setAttribute(Constants.SESSION_USER_ID, null);
        UserService.UserCreatationResult userCreatationResult =
                userService.createUser(username, password, inviteKey);
        if (userCreatationResult.isResult()) {
            result.put("result","failed");
            result.put("message", userCreatationResult.getMessage());
        } else {
            result.put("result","success");
            session.setAttribute(Constants.SESSION_USER_ID, userCreatationResult.getUserId());
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
