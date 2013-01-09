/*
 * Distributed as part of c3p0 v.0.9.2
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.c3p0.test;

import java.sql.*;
import javax.sql.*;
import com.mchange.v2.c3p0.*;

public final class InterruptedBatchTest
{
    static DataSource ds_unpooled = null;
    static DataSource ds_pooled   = null;

    public static void main(String[] argv)
    {
        if (argv.length > 0)
        {
            System.err.println( C3P0BenchmarkApp.class.getName() + 
                                " now requires no args. Please set everything in standard c3p0 config files.");
            return;                    
        }

	try
	    {
		ds_unpooled = new DriverManagerDataSource();
 		ComboPooledDataSource cpds = new ComboPooledDataSource();
 		ds_pooled = cpds;

		attemptSetupTable();

		performTransaction( true );
		performTransaction( false );

		checkCount();
	    }
	catch( Throwable t )
	    {
		System.err.print("Aborting tests on Throwable -- ");
		t.printStackTrace(); 
		if (t instanceof Error)
		    throw (Error) t;
	    }
	finally
	    {
 		try { DataSources.destroy(ds_pooled); }
		catch (Exception e)
		    { e.printStackTrace(); }

 		try { DataSources.destroy(ds_unpooled); }
		catch (Exception e)
		    { e.printStackTrace(); }
	    }
    }

    public static void performTransaction(boolean throwAnException) throws SQLException
    {
	Connection        con        = null;
	PreparedStatement prepStat   = null;
	
	try
	    {
		con = ds_pooled.getConnection();
		con.setAutoCommit(false);

		prepStat = con.prepareStatement("INSERT INTO CG_TAROPT_LOG(CO_ID, ENTDATE, CS_SEQNO, DESCRIPTION) VALUES (?,?,?,?)");
		
		prepStat.setLong     (1, -665);
		prepStat.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
		prepStat.setInt      (3, 1);
		prepStat.setString   (4, "time: " + System.currentTimeMillis());
		
		prepStat.addBatch();
		
		if(throwAnException)
		    throw new NullPointerException("my exception");
		
		prepStat.executeBatch();
		
		con.commit();
	    }
	catch(Exception e)
	    {
		System.out.println("exception caught (NPE expected): " /* + e */);
		e.printStackTrace();
	    }
	finally
	    {
		try { if (prepStat != null) prepStat.close(); } catch (Exception e) { e.printStackTrace(); }
		try { con.close(); } catch (Exception e) { e.printStackTrace(); }
	    }		
    }

    private static void attemptSetupTable() throws Exception
    {
	Connection con = null;
	Statement stmt = null;
	try
	    {
		con = ds_pooled.getConnection();
		stmt = con.createStatement();
		try
		    {
			stmt.executeUpdate("CREATE TABLE CG_TAROPT_LOG ( CO_ID INTEGER, ENTDATE TIMESTAMP, CS_SEQNO INTEGER, DESCRIPTION VARCHAR(32) )");
		    }
		catch (SQLException e) 
		    {
			System.err.println("Table already constructed?");
			e.printStackTrace(); 
		    }

		stmt.executeUpdate("DELETE FROM CG_TAROPT_LOG");
	    }
	finally
	    {
		try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
		try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
	    }
    }

    private static void checkCount() throws Exception
    {
	Connection con  = null;
	Statement  stmt = null;
	ResultSet  rs   = null;
	try
	    {
		con  = ds_pooled.getConnection();
		stmt = con.createStatement();

		rs = stmt.executeQuery("SELECT COUNT(*) FROM CG_TAROPT_LOG");
		rs.next();
		System.out.println( rs.getInt(1) + " rows found. (one row expected.)" );
	    }
	finally
	    {
		try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
		try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
	    }
    }
}





