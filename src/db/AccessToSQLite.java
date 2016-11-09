package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class AccessToSQLite {
    private String sConnect;
    private Connection connection = null;
    private Logger logger;

    public AccessToSQLite(Logger log, String sConnect) {
        logger = log;
        this.sConnect = sConnect;
    }

    public boolean getAccess(){
        return  connectToSQLite();
    }

    private boolean connectToSQLite() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.info("connectToSQLite: SQLite JDBC драйвер не найден");
            return false;
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:"+sConnect);
            logger.info("connectToSQLite: БД подключена через JDBC драйвер");
        } catch (SQLException e) {
            logger.info("connectToSQLite: ошибка подключения к БД через JDBC драйвер");
            return false;
        }
        return true;
    }

    public boolean closeAccess(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Connection getConnection() {
        return connection;
    }
}
