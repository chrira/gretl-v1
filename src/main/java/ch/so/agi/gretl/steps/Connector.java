package ch.so.agi.gretl.steps;


import ch.so.agi.gretl.util.DbConnector;


import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Class which is used get a connection to the database
 */
public class Connector {

    private String dbUri;
    private String dbUser;
    private String dbPassword;
    private GretlLogger log;
    private Connection dbConnection = null;

    public Connector(String dbUri) {
        this(dbUri, null, null);
    }

    public Connector(String dbUri, String dbUser) {
        this(dbUri, dbUri, null);
    }

    public Connector(String dbUri, String dbUser, String dbPassword) {
        this.dbUri = dbUri;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.log = LogEnvironment.getLogger(this.getClass());
    }

    public Connection connect() throws SQLException {
        if (dbConnection == null) {
            dbConnection = DbConnector.connect(dbUri, dbUser, dbPassword);
            dbConnection.setAutoCommit(false);
        }
        return dbConnection;

    }



}
