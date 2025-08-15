package org.leolo.web.nrinfo.dao.networkrail;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.networkrail.Corpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Collection;

@Component
public class CorpusDao extends BaseDao {

    private Logger logger = LoggerFactory.getLogger(CorpusDao.class);

    @Autowired
    private DataSource dataSource;

    public void truncateAndAddAll(Collection<Corpus> corpus) {
        try(Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            //truncate the data and reset auto increment
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("TRUNCATE TABLE corpus");
            }
            try(PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO corpus (tiploc, stanox_code, uic_code, crs_code, nlc_code, short_name, long_name) VALUES (?,?,?,?,?,?,?)"
            )){
                int count = 0;
                for (Corpus c : corpus) {
                    setString(ps, 1, c.getTiploc());
                    setString(ps, 2, c.getStanox());
                    setString(ps, 3, c.getUicCode());
                    setString(ps, 4, c.getCrsCode());
                    setString(ps, 5, c.getNlc());
                    setString(ps, 6, c.getShortName());
                    setString(ps, 7, c.getLongName());
                    ps.addBatch();
                    if (++count % BATCH_SIZE == 0) {
                        ps.executeBatch();
                        connection.commit();
                        logger.info("Execute a batch of CORPUS, total size = {}", count);
                    }
                }
                ps.executeBatch();
                logger.info("Execute final batch of CORPUS, total size = {}", count);
            }
            connection.commit();
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
