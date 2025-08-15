package org.leolo.web.nrinfo.service;

import org.leolo.web.nrinfo.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;

@Component
public class UserService {

    private Logger logger = LoggerFactory.getLogger(UserService.class);

    public static final int INVALID_USER = -1;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ConfigurationService configurationService;

    public class UserCreatationResult {
        private int userId;
        private boolean result;
        private String message;

        public int getUserId() {
            return userId;
        }

        public boolean isResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }

        private UserCreatationResult(int userId, boolean result, String message) {
            this.userId = userId;
            this.result = result;
            this.message = message;
        }
    }

    public UserCreatationResult createUser(String userName, String password) {
        return createUser(userName, password, null);
    }

    public UserCreatationResult createUser(String userName, String password, String inviteKey) {
        //Check username and password format
        if (!userName.matches("^[a-zA-Z0-9]{4,100}$")) {
            logger.debug("Invalid username {}", userName);
            return new UserCreatationResult(-1, false, "Username must between 4 and 100 character, and alphanumerical only");
        }
        if (password.length() >= 6 && password.getBytes().length <= 71) {
            logger.debug("Invalid password length, {} char/{} B", password.length(), password.getBytes().length);
            return new UserCreatationResult(-1, false, "Password must be at least 6 character, and less than 71 bytes");
        }
        if (configurationService.getConfiguration(
                "user.allowRegister", "false").equalsIgnoreCase("true") &&
                (inviteKey == null || inviteKey.strip().length() == 0)
        ) {
            logger.debug("Missing invite key but open registration is disabled");
            return new UserCreatationResult(-1, false, "invite key is required");
        }
        try(Connection conn = dataSource.getConnection()){
            long registerTime = System.currentTimeMillis();
            //Check invite key
            if (inviteKey == null || inviteKey.strip().length() == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM user_invite_key " +
                                "WHERE invite_key = ? AND assigned_user is null and " +
                                "(expiry_date is null or expiry_date >= ?)"
                )) {
                    ps.setString(1, inviteKey);
                    ps.setTimestamp(2, new Timestamp(registerTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            logger.debug("Invalid invite key");
                            return new UserCreatationResult(-1, false, "Invalid invite key");
                        }
                    }
                }
            }
            //Check username
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM user WHERE username = ?")) {
                ps.setString(1, userName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        logger.debug("Duplicated username");
                        return new UserCreatationResult(-1, false, "Username already exists");
                    }
                }
            }
            //Create the user
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user (username, created_date) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setString(1, userName);
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
                    return new UserCreatationResult(id, true, "User created");
                }
            }
        } catch (SQLException e){
            logger.error("Unable to create user - {}", e.getMessage(),e);
            return new UserCreatationResult(-1, false, "System error");
        }
        return new UserCreatationResult(-1, false, "System error");
    }

    public int login (String username, String password) {
        try (Connection connection = dataSource.getConnection()){
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT user.user_id, user_password.password " +
                            "FROM user JOIN user_password on user.user_id = user_password.user_id " +
                            "WHERE username = ?"
            )) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()){
                        if (BCrypt.checkpw(password, rs.getString(2))){
                            return rs.getInt(1);
                        } else {
                            return INVALID_USER;
                        }
                    } else {
                        // No such user
                        BCrypt.hashpw(password, BCrypt.gensalt());
                        return INVALID_USER;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("System error - {}", e.getMessage(), e);
        }
        return INVALID_USER;
    }

    public void saveMiscData(int userId, String keyName, Serializable data) {
        if (keyName==null||data==null) {
            throw new IllegalArgumentException();
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "REPLACE  INTO user_misc_data (user_id, data_index, data_type, data) values (?,?,?,?)"
                )
        ) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, keyName);
            pstmt.setString(3, data.getClass().getName());
            try(
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos)
            ) {
                oos.writeObject(data);
                try(ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())){
                    pstmt.setBinaryStream(4, bais);
                    pstmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            logger.error("Unable to insert misc data - {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Unable to insert misc data - {}", e.getMessage(), e);
        }
    }

    public Object getMiscData(int userId, String keyName){
        return getMiscData(userId, keyName, Serializable.class);
    }

    public <T extends Serializable> T getMiscData(int userId, String keyName, Class<T> targetClass){
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT data_type, data, data_size FROM user_misc_data WHERE user_id = ? AND data_index = ?"
                )
        ){
            pstmt.setInt(1, userId);
            pstmt.setString(2, keyName);
            try(ResultSet rs = pstmt.executeQuery()){
                if (rs.next()) {
                    String dataType = rs.getString(1);
                    Blob data = rs.getBlob(2);
                    int dataSize = rs.getInt(3);
                    Object obj = null;
                    try (ObjectInputStream ois = new ObjectInputStream(data.getBinaryStream())){
                        obj = ois.readObject();
                    }
                    if (targetClass.isInstance(obj)) {
                        return (T)obj;
                    } else {
                        logger.error("Deserialization error - class mismatch. {} requested. {} expected. {} found.",
                                dataType,
                                targetClass.getName(),
                                obj.getClass().getName());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error - {} ", e.getMessage(), e);
        } catch (IOException|ClassNotFoundException e) {
            logger.error("I/O error - {} ", e.getMessage(), e);
        }
        return null;
    }

    public boolean hasMiscData(int userId, String keyName) {
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT 1 FROM user_misc_data WHERE user_id = ? AND data_index = ?"
                )
        ){
            pstmt.setInt(1, userId);
            pstmt.setString(2, keyName);
            try(ResultSet rs = pstmt.executeQuery()){
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Database error - {} ", e.getMessage(), e);
        }
        return false;
    }
}
