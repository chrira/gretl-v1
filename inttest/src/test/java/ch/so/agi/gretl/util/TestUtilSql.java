package ch.so.agi.gretl.util;

import java.sql.*;

/**
 * Contains helper methods for the test's of the Db2DbTask and SqlExecutorTask
 */
public class TestUtilSql {
    public static final String VARNAME_PG_CON_URI = "gretltest_dburi";
    public static final String PG_CON_URI = System.getProperty(VARNAME_PG_CON_URI);//"jdbc:postgresql://localhost:5432/gretl"

    public static final String PG_CON_USER = "ddluser";
    public static final String PG_CON_PASS = "ddluser";

    private static void dropSchema(String schemaName, Connection con) throws SQLException {
        if(con == null){ return; }

        Statement s = con.createStatement();
        s.execute(String.format("drop schema %s cascade", schemaName));
    }

    public static void closeCon(Connection con){
        try {
            if(con != null)
                con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection connectPG(){
        Connection con = null;
        try {
            Driver pgDriver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
            DriverManager.registerDriver(pgDriver);

            con = DriverManager.getConnection(
                    PG_CON_URI,
                    PG_CON_USER,
                    PG_CON_PASS);

            con.setAutoCommit(false);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }

        return con;
    }

    public static void createOrReplaceSchema(Connection con, String schemaName){

        try {
            Statement s = con.createStatement();
            s.addBatch(String.format("drop schema if exists %s cascade", schemaName));
            s.addBatch("create schema " + schemaName);
            s.addBatch(String.format("grant usage on schema %s to dmluser", schemaName));
            s.addBatch(String.format("grant usage on schema %s to readeruser", schemaName));
            s.executeBatch();
            con.commit();
        }
        catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int execCountQuery(Connection con, String query){
        Statement s = null;
        int count = -1;
        try{
            s = con.createStatement();
            ResultSet rs = s.executeQuery(query);
            rs.next();
            count = rs.getInt(1);

            if(count == -1)
                throw new RuntimeException(String.format("Query [%s] did not return valid row count",query));
        }
        catch (SQLException se){
            throw new RuntimeException(se);
        }
        finally {
            if(s != null){
                try{
                    s.close();
                }
                catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        return count;
    }

    public static void grantTableModsInSchemaToUser(Connection con, String schemaName, String userName){

        String sql = String.format("grant select, insert, update, delete on all tables in schema %s to %s", schemaName, userName);
        //grant permissions to dmluser on all objects in the schema
        Statement s = null;
        try {
            s = con.createStatement();
            s.execute(String.format("grant select, insert, update, delete on all tables in schema %s to dmluser", schemaName));
            s.close();
        }
        catch (SQLException se){
            throw new RuntimeException(se);
        }
        finally {
            if(s != null){
                try{
                    s.close();
                }
                catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
