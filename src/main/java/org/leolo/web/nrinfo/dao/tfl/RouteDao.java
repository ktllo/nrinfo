package org.leolo.web.nrinfo.dao.tfl;

import org.leolo.web.nrinfo.dao.BaseDao;
import org.leolo.web.nrinfo.model.tfl.Route;
import org.leolo.web.nrinfo.model.tfl.ServiceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@Component
public class RouteDao extends BaseDao {

    private Logger logger = LoggerFactory.getLogger(RouteDao.class);

    @Autowired
    private DataSource dataSource;

    public List<Route> getRoutes() {
        List<Route> routes = new Vector<>();
        String sql = "select * from tfl_route";
        try (
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ){
            while (rs.next()) {
                routes.add(parseRoute(rs));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return routes;
    }

    public List<Route> getRoutesByLineId(String lineId) {
        List<Route> routes = new Vector<>();
        String sql = "select * from tfl_route where line_id=?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ){
            stmt.setString(1, lineId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    routes.add(parseRoute(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return routes;
    }

    public List<Route> getRoutesByServiceMode(ServiceMode mode) {
        List<Route> routes = new Vector<>();
        String sql = "select * from tfl_route where line_id in (select line_id from tfl_line where mode_code=?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, mode.getModeCode());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    routes.add(parseRoute(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return routes;
    }

    public Route getRouteByRouteId(String routeId) {
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("select * from tfl_route where route_id=?")
        ){
            stmt.setString(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parseRoute(rs);
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public Route getRoutByRouteId(UUID routeId) {
        return getRouteByRouteId(routeId.toString());
    }

    public UUID matchesRoute(Route route) {
        if(route.getRouteId() != null) {
            logger.warn("Route already have UUID assigned - {}", route.getRouteId());
            return route.getRouteId();
        }
        String sql = "select route_id from tfl_route where line_id=? and direction=? and origin=? and destination=?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, route.getLineId());
            pstmt.setString(2, route.getDirection());
            pstmt.setString(3, route.getOrigin());
            pstmt.setString(4, route.getDestination());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("route_id"));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public void insertOrUpdateRoute(Route route) {
        if(route.getRouteId() == null) {
            //We don't have route ID yet
            route.setRouteId(matchesRoute(route));
        }
        boolean insert =  (route.getRouteId() == null || !isUUIDExist(route.getRouteId()));
        if (insert && route.getRouteId() == null) {
            route.setRouteId(UUID.randomUUID());
        }
        String sql;
        if (insert) {
            sql = "INSERT INTO tfl_route (line_id, direction, origin, destination, route_id) VALUES (?, ?, ?, ?, ?)";
            logger.debug("Inserting route {} ({},{},{}>{}) to tfl_route",
                    route.getRouteId(), route.getLineId(), route.getDirection(), route.getOrigin(), route.getDestination());
        } else {
            sql = "UPDATE tfl_route SET line_id = ?, direction = ?, origin = ?, destination = ? WHERE route_id = ?";
            logger.debug("Updating route {} ({},{},{}>{}) to tfl_route",
                    route.getRouteId(), route.getLineId(), route.getDirection(), route.getOrigin(), route.getDestination());
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, route.getLineId());
            pstmt.setString(2, route.getDirection());
            pstmt.setString(3, route.getOrigin());
            pstmt.setString(4, route.getDestination());
            pstmt.setString(5, route.getRouteId().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private boolean isUUIDExist(UUID uuid) {
        return isUUIDExist(uuid.toString());
    }

    private boolean isUUIDExist(String uuid) {
        String sql = "select 1 from tfl_route where route_id=?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    private Route parseRoute(ResultSet rs) throws SQLException {
        Route route = new Route();
        route.setRouteId(UUID.fromString(rs.getString("route_id")));
        route.setLineId(rs.getString("line_id"));
        route.setDirection(rs.getString("direction"));
        route.setOrigin(rs.getString("origin"));
        route.setDestination(rs.getString("destination"));
        return route;
    }

}
