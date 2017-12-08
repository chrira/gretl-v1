package ch.so.agi.gretl.jobs;

import ch.so.agi.gretl.util.GradleVariable;
import ch.so.agi.gretl.util.TestUtil;
import ch.so.agi.gretl.util.TestUtilSql;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.*;

public class Db2DbTaskTest {

    /*
    Test's that a chain of statements executes properly and results in the correct
    number of inserts (corresponding to the last statement)
        1. Statement transfers rows from a to b
        2. Statement transfers rows from b to a
    */
    @Test
    public void db2dbTaskChainTest() throws Exception {
        String schemaName = "db2dbTaskChain".toLowerCase();
        Connection con = null;
        try{
            con = TestUtilSql.connectPG();
            TestUtilSql.createOrReplaceSchema(con, schemaName);
            int countSrc = prepareDb2DbChainTables(con, schemaName);
            con.commit();
            TestUtilSql.closeCon(con);

            GradleVariable[] gvs = {GradleVariable.newGradleProperty(TestUtilSql.VARNAME_PG_CON_URI, TestUtilSql.PG_CON_URI)};
            TestUtil.runJob("jobs/db2dbTask", gvs);

            //reconnect to check results
            con = TestUtilSql.connectPG();
            String countDestSql = String.format("select count(*) from %s.albums_dest", schemaName);
            int countDest = TestUtilSql.execCountQuery(con, countDestSql);

            Assert.assertEquals(
                    "Rowcount in table albums_src must be equal to rowcount in table albums_dest",
                    countSrc,
                    countDest);
        }
        finally {
            TestUtilSql.closeCon(con);
        }
    }

    private static final int prepareDb2DbChainTables(Connection con, String schemaName){
        int srcRowCount = 4;


        String ddlBase = "CREATE TABLE %s.albums_%s(" +
                "title text, artist text, release_date text," +
                "publisher text, media_type text)";

        try{
            //source table
            Statement s1 = con.createStatement();
            System.out.println(String.format(ddlBase, schemaName, "src"));
            s1.execute(String.format(ddlBase, schemaName, "src"));
            s1.close();

            PreparedStatement ps = con.prepareStatement(
                    String.format("INSERT INTO %s.albums_src VALUES (?,?,?,?,?)", schemaName)
            );

            String[] row = {"Exodus", "Andy Hunter", "7/9/2002", "Sparrow Records", "CD"};
            for(int i=0; i<srcRowCount; i++){
                for(int j=0; j<row.length; j++){
                    ps.setString(j+1, row[j]);
                }
                ps.executeUpdate();
            }
            ps.close();

            //dest table
            Statement s2 = con.createStatement();
            s2.execute(String.format(ddlBase, schemaName,"dest"));
            s2.close();

            //intermediate table
            Statement s3 = con.createStatement();
            s3.execute(String.format(ddlBase, schemaName,"intermediate"));
            s3.close();

            TestUtilSql.grantTableModsInSchemaToUser(con, schemaName, "dmlUser");
        }
        catch(SQLException se){
            throw new RuntimeException(se);
        }

        return srcRowCount;
    }
}
