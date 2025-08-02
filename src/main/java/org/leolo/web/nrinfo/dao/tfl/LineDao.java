package org.leolo.web.nrinfo.dao.tfl;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.tfl.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Vector;

@Component
public class LineDao extends BaseDao {

    private Logger logger = LoggerFactory.getLogger(LineDao.class);

    @Autowired
    private DataSource dataSource;

    public List<Line> getLines() {
        List<Line> lines = new Vector<>();
        try(
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select * from tfl_line")
        ) {
            while (rs.next()) {
                lines.add(parseResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return lines;
    }

    public Line getLineByLineId(String lineId) {
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT * FROM tfl_line WHERE line_id=?"
                )
        ){
            pstmt.setString(1, lineId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return parseResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public boolean hasLineId(String lineId) {
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("select 1 from tfl_line where line_id=?")
        ){
            pstmt.setString(1, lineId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public void insertOrUpdateLine(Line line) {
        if (line.getLineId() == null || line.getLineId().isEmpty()) {
            throw new IllegalArgumentException("line id is empty");
        }
        String sql;
        if (!hasLineId(line.getLineId())) {
            sql = "INSERT INTO tfl_line (line_name, mode_code, line_id) VALUES (?, ?, ?)";
            logger.debug("Inserting new line {} into table tfl_line", line.getLineId());
        } else {
            sql = "UPDATE tfl_line SET line_name=?, mode_code=? WHERE line_id=?";
            logger.debug("Updating line {} for table tfl_line", line.getLineId());
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, line.getLineName());
            pstmt.setString(2, line.getModeCode());
            pstmt.setString(3, line.getLineId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Line parseResultSet(ResultSet rs) throws SQLException {
        Line line = new Line();
        line.setLineId(rs.getString("line_id"));
        line.setLineName(rs.getString("line_name"));
        line.setModeCode(rs.getString("mode_code"));
        return line;
    }
}
