/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.config.data;

import itensil.io.HibernateUtil;
import itensil.security.hibernate.SignOnHB;
import itensil.util.Check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Some parts of this class are from Apache Ant - 
 * 
 * Under license:
 * 
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  
 * Other parts:
 * 
 * @author ggongaware@itensil.com
 *
 */
public class SqlLoader {

	protected static Logger logger = Logger.getLogger(SqlLoader.class);
	
	private int goodSql = 0;
    private int totalSql = 0;
    
    /**
     * Database connection
     */
    private Connection conn = null;
	
    /**
     * SQL statement
     */
    private Statement statement = null;
    
    /**
     * Ctor
     */
    public SqlLoader() {	
    }
    
    
    /**
     * Execute a SQL script
     * 
     * @param name - resource file name
     * 
     * Wraps itself in hibernate transaction
     * 
     * @throws SQLException
     * @throws IOException
     */
    public synchronized void execResource(String name) throws SQLException, IOException {
    	InputStream ins = getClass().getResourceAsStream(name);
    	HibernateUtil.beginTransaction();
    	conn = HibernateUtil.getSession().connection();
    	statement = conn.createStatement();
    	runStatements(new InputStreamReader(ins));
    	HibernateUtil.commitTransaction();
    	statement.close();
    	HibernateUtil.closeSession();
    }
    
	/**
     * read in lines and execute them
     * @param reader the reader contains sql lines.
     * @throws SQLException on sql problems
     * @throws IOException on io problems
     */
    protected void runStatements(Reader reader) throws SQLException, IOException {
    	
        StringBuffer sql = new StringBuffer();
        String line;

        BufferedReader in = new BufferedReader(reader);

        while ((line = in.readLine()) != null) {
                line = line.trim();

            if (line.startsWith("//")
            		|| line.startsWith("--")
            		|| line.startsWith("#")) {
                continue;
            }
            
            sql.append(" ");
            sql.append(line);
            
            // SQL defines "--" as a comment to EOL
            // and in Oracle it may contain a hint
            // so we cannot just remove it, instead we must end it
            if (line.indexOf("--") >= 0) {
                sql.append("\n");
            }
            int len = sql.length();
            if (sql.charAt(len - 1) == ';') {
                execSQL(sql.substring(0, len - 1));
                sql.setLength(0);
            }
        }
        // Catch any statements not followed by ;
        if (sql.length() > 0) {
            execSQL(sql.toString());
        }
    }


    /**
     * Exec the sql statement.
     * @param sql the SQL statement to execute
     * @throws SQLException on SQL problems
     */
    protected void execSQL(String sql) throws SQLException {
    	sql = sql != null ? sql.trim() : null;
    	if (Check.isEmpty(sql)) return;
  	  	ResultSet resultSet = null;
		try {
			totalSql++;
			logger.debug("SQL: " + sql);

			boolean ret;
			int updateCount = 0, updateCountTotal = 0;

			ret = statement.execute(sql);
			updateCount = statement.getUpdateCount();
			resultSet = statement.getResultSet();
			do {
				if (!ret) {
					if (updateCount != -1) {
						updateCountTotal += updateCount;
					}
				}
				ret = statement.getMoreResults();
				if (ret) {
					updateCount = statement.getUpdateCount();
					resultSet = statement.getResultSet();
				}
			} while (ret);

			logger.debug(updateCountTotal + " rows affected");

			SQLWarning warning = conn.getWarnings();
			while (warning != null) {
				logger.warn(warning + " sql warning");
				warning = warning.getNextWarning();
			}
			conn.clearWarnings();
			goodSql++;
		} catch (SQLException e) {
			logger.error("Failed to execute: " + sql, e);
		} finally {
			if (resultSet != null) {
				resultSet.close();
			}
		}
    }
}
