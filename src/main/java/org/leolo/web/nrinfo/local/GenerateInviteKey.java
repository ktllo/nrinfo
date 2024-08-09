package org.leolo.web.nrinfo.local;

import scala.concurrent.java8.FuturesConvertersImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.Random;

public class GenerateInviteKey {

    public static final int DEFAULT_GENERATE_COUNT = 32;
    public static final int KEY_LENGTH = 24;

    public static final char [] KEY_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    public static void main(String[] args) throws Exception{
        Properties props = new Properties();
        props.load(GenerateInviteKey.class.getResourceAsStream("/application.properties"));
        try(
                Connection conn = DriverManager.getConnection(
                    props.getProperty("spring.datasource.url"),
                    props.getProperty("spring.datasource.username"),
                    props.getProperty("spring.datasource.password")
                );
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO user_invite_key (invite_key, generate_by, generated_date) VALUES (?, ?, NOW())"
                )
        ){
            Random random = new Random();
            ps.setString(2, "Standlone Generator");
            for(int i=0; i<DEFAULT_GENERATE_COUNT; i++){
                StringBuilder key = new StringBuilder();
                for (int j=0; j<KEY_LENGTH; j++){
                    key.append(KEY_CHARS[random.nextInt(KEY_CHARS.length)]);
                }
                System.out.println("Generated key : "+key.toString());
                ps.setString(1, key.toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
