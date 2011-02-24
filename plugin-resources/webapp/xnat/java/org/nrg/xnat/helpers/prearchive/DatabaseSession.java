package org.nrg.xnat.helpers.prearchive;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * DatabaseSession is an abstraction over how a session is represented in the 
 * prearchive table. Every enum is a column in the database and holds its name 
 * and data type. Each enum also provides convenience methods for inserting 
 * itself into prepared SQL statements and reading its value from the result of 
 * a query.
 * 
 * The order in which the enums appear is the same as the order of columns 
 * in the database table.
 * 
 * Most of these columns also map from/to the slots of a session object. 
 * @author aditya siram
 */
public enum DatabaseSession {
	// a project column is allowed to hold a null value to indicate
	// an unassigned project.
	PROJECT("project", ColType.PROJECTNAME, true){
		@Override
		public Object readSession (SessionData s) {
			return s.getProject();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setProject(o);
		}
		@Override
		public String searchSql (Object o) {
			if (null == o|| ((String)o).equals(PrearcUtils.COMMON)) {
				return this.nullSql();
			}
			else {
				return this.eqSql(this.getColumnType().typeToString(o));
			}
		}
	},
	TIMESTAMP("timestamp", ColType.VARCHAR, false){
		@Override
		public Object readSession(SessionData s) {
			return s.getTimestamp();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setTimestamp(o);
		}
	},
	
	LASTMOD("lastmod", ColType.TIMESTAMP, true){
		@Override
		public Object readSession (SessionData s) {
			return s.getLastBuiltDate();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setLastBuiltDate(o);
		}
	},
	UPLOADED("uploaded", ColType.TIMESTAMP, true){
		@Override
		public Object readSession (SessionData s) {
			return s.getUploadDate();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setUploadDate(o);
		}
	},
	SCANDATE("scan_date", ColType.TIMESTAMP, true){
		@Override
		public Object readSession (SessionData s) {
			return s.getScan_date();
		}
		@Override
		
		public void writeSession (SessionData s, Object o) {
			s.setScan_date(o);
		}
	},
	
	SCANTIME("scan_time", ColType.VARCHAR, true){
		@Override
		public Object readSession (SessionData s) {
			return s.getScan_time();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setScan_time(o);
		}
	},
	SUBJECT("subject", ColType.VARCHAR, true){
		@Override
		public Object readSession (SessionData s) {
			return s.getSubject();
		}
		@Override
		public void writeSession (SessionData s,Object o) {
			s.setSubject(o);
		}
	},
	FOLDER_NAME("folderName", ColType.VARCHAR, false){
		@Override
		public Object readSession (SessionData s){
			return s.getFolderName();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setFolderName(o);
		}
	},
	NAME("name", ColType.VARCHAR, true){
		@Override
		public Object readSession (SessionData s){
			return s.getName();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setName(o);
		}
	},
	TAG("tag", ColType.VARCHAR, true){
		@Override
		public Object readSession (SessionData s){
			return s.getTag();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setTag(o);
		}
	},
	STATUS("status", ColType.STATUS, false){
		@Override
		public Object readSession (SessionData s){
			return s.getStatus();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setStatus(o);
		}
		@Override
		public String updateSessionSql (String sess, String timestamp, String proj, Object newVal) {
			String s =  "UPDATE " + PrearcDatabase.table + " SET " + 
			       this.searchSql(newVal) +
			       ", " + DatabaseSession.LASTMOD.searchSql(Calendar.getInstance().getTime()) + 
			       " WHERE " + DatabaseSession.sessionSql(sess, timestamp, proj);
			return s;
		}
	}
	,
	URL("url", ColType.VARCHAR, false){
		@Override
		public Object readSession (SessionData s){
			return s.getUrl();
		}
		@Override
		public void writeSession (SessionData s, Object o) {
			s.setUrl(o);
		}
	}
	;
	
	/**
	 * ColType provides a simple mapping from Java objects to java.sql.* objects   
	 * @author aditya siram
	 *
	 */
	private enum ColType {
		PROJECTNAME () {
			@Override
			public void setInsertStatement(int columnIndex, PreparedStatement s, Object o) throws SQLException {
				if (((String) o).equals(PrearcUtils.COMMON)) {
					ColType.VARCHAR.setInsertStatement(columnIndex, s, null);	
				}
				else {
					ColType.VARCHAR.setInsertStatement(columnIndex, s, o);	
				}
				
			}
			@Override
			@SuppressWarnings("unchecked")
			public <T extends Object> T getFromResult(int columnIndex, ResultSet r) throws SQLException {
				Object res = ColType.VARCHAR.getFromResult(columnIndex, r);
				if (null == res) {
					return (T) PrearcUtils.COMMON;
				}
				else {
					return (T) res;
				}
			}
			@Override
			public String resultToString (int columnIndex, ResultSet r) throws SQLException {
				return ColType.VARCHAR.resultToString(columnIndex,r);
			}
			@Override
			public String toString () {
				return ColType.VARCHAR.toString();
			}
			@Override
			public String typeToString (Object o) {
				return ColType.VARCHAR.typeToString(o);
			}
		},
		STATUS () {
			@Override
			public void setInsertStatement(int columnIndex, PreparedStatement s, Object o) throws SQLException {
				if (o != null) {
					s.setString(columnIndex, ((PrearcUtils.PrearcStatus)o).name());
				}
				else {
					s.setNull(columnIndex, java.sql.Types.VARCHAR);
				}
			}
			@Override
			@SuppressWarnings("unchecked")
			public <T extends Object> T getFromResult(int columnIndex, ResultSet r) throws SQLException {
				return (T) PrearcUtils.PrearcStatus.valueOf(r.getString(columnIndex));
			}
			@Override
			public String typeToString (Object o) {
				return null == o ? null : DatabaseSession.singleQuote(((PrearcUtils.PrearcStatus)o).name());
			}
			@Override
			public String resultToString (int columnIndex, ResultSet r) throws SQLException {
				return ColType.VARCHAR.resultToString(columnIndex, r);
			}
			@Override
			public String toString() {
				return ColType.VARCHAR.toString();
			}
			
		},
		VARCHAR () {
			@Override
			public void setInsertStatement(int columnIndex, PreparedStatement s, Object o) throws SQLException {
				if (o != null) {
					s.setString(columnIndex, ((String)o).toString());
				}
				else {
					s.setNull(columnIndex, java.sql.Types.VARCHAR);
				}
			}
			@Override
			@SuppressWarnings("unchecked")
			public <T extends Object> T getFromResult(int columnIndex, ResultSet r) throws SQLException {
				return (T) r.getString(columnIndex);
			}
			@Override
			public String typeToString(Object o) {
				return null == o ? null : DatabaseSession.singleQuote(((String)o).toString()); 
			} 
			@Override
			public String resultToString (int columnIndex, ResultSet r) throws SQLException {
				return this.typeToString(r.getString(columnIndex));
			}
		},
		TIMESTAMP () {
			@Override
			public void setInsertStatement(int columnIndex, PreparedStatement s, Object o) throws SQLException {
				if (o != null) {
					if (o instanceof java.util.Date) {
						s.setTimestamp(columnIndex, new java.sql.Timestamp(((java.util.Date)o).getTime()));}
					else {
						s.setTimestamp(columnIndex, this.date2Timestamp(o));
					}
				}
				else {
					s.setNull(columnIndex, java.sql.Types.TIMESTAMP);
				}
			}

			public java.sql.Timestamp date2Timestamp (Object o) {
				java.util.Date d = (java.util.Date) o;
				return new java.sql.Timestamp(d.getTime());
			}
			@Override
			@SuppressWarnings("unchecked")
			public <T extends Object> T getFromResult(int columnIndex, ResultSet r) throws SQLException {
				return (T) r.getTimestamp(columnIndex);
			}
			@Override
			public String typeToString (Object o) {
				return null == o ? null : DatabaseSession.singleQuote(this.date2Timestamp(o).toString());
			}
			@Override
			public String resultToString (int columnIndex, ResultSet r) throws SQLException {
				return this.typeToString(r.getTimestamp(columnIndex));
			}
		}, 
		BOOL {
			@Override
			public void setInsertStatement(int columnIndex,
					PreparedStatement s, Object o) throws SQLException {
				if (o != null) {
					if (o instanceof Boolean) {
						s.setBoolean(columnIndex, (Boolean)o);
					}
				}
				else {
					s.setNull(columnIndex, java.sql.Types.BOOLEAN);
				}
				
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> T getFromResult(int columnIndex, ResultSet r)
					throws SQLException {
				return (T) (Boolean) r.getBoolean(columnIndex);
			}

			@Override
			public String resultToString(int columnIndex, ResultSet r)
					throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String typeToString(Object o) {
				return null == o ? null : DatabaseSession.singleQuote(((Boolean)o).toString());
			}
		};
		/**
		 * Insert the given object into the prepared statement.
		 * @param columnIndex
		 * @param s
		 * @param o
		 * @throws SQLException
		 */
		public abstract void setInsertStatement(int columnIndex, PreparedStatement s, Object o) throws SQLException;
		/**
		 * Extract this columns' data from the given row. 
		 * @param <T>
		 * @param columnIndex
		 * @param r
		 * @return
		 * @throws SQLException
		 */
		public abstract <T extends Object> T getFromResult(int columnIndex, ResultSet r) throws SQLException;
		/**
		 * Extract this columns data from the given row and convert it to a string.
		 * @param columnIndex
		 * @param r
		 * @return
		 * @throws SQLException
		 */
		public abstract String resultToString (int columnIndex, ResultSet r) throws SQLException;
		/**
		 * Convert this columns data to a string. Returns null if the given object is null.
		 * @param o
		 * @return
		 */
		public abstract String typeToString (Object o);
	}
	private String columnName;
	private DatabaseSession.ColType columnType;
	private boolean canBeNull;
	
	/**
	 * Construct the representation of a database column 
	 * @param columnName The name of the column (no spaces) 
	 * @param columnType The kind of value this column can hold
	 * @param canBeNull  Whether this column can hold a null value
	 */
	private DatabaseSession (String columnName, DatabaseSession.ColType columnType, boolean canBeNull) {
		this.columnName = columnName;
		this.columnType = columnType;
		this.canBeNull = canBeNull;
	}
	public DatabaseSession.ColType getColumnType () {
		return this.columnType;
	}
	public String getColumnName () {
		return this.columnName;
	}
	
	

	/**
	 * Generate the <column=value> SQL
	 * eg. "SELECT ... FROM ... WHERE <column=value>"
	 */
	public String eqSql (String value) {
		return this.getColumnName() + "=" + value;
	}
	/**
	 * Generate the "column IS NULL" in eg. "SELECT ... FROM ... WHERE column IS NULL"
	 * @return
	 */
	public String nullSql () {
		return this.getColumnName() + " IS NULL";
	}
	
	/**
	 * Generate the <column selection> SQL in eg. "SELECT ... FROM ... WHERE <column selection>.
	 * The value to select on is pulled directly from the given Session object. 
	 * @param s
	 * @return
	 */
	public String searchSql (SessionData s) {
		return searchSql(this.readSession(s));
	}
	
	/**
	 * Generate the <column selection> SQL 
	 * eg. "SELECT ... FROM ... WHERE <column selection>
	 * @param value value of column
	 * @return
	 */
	public String searchSql (Object value) {
		String obj = this.columnType.typeToString(value);
		if (null == obj || obj.isEmpty()) {
			return this.nullSql();
		}
		else {
			return this.eqSql(obj);
		}
	}
	
	/**
	 * Generate <column selection> SQL for all columns 
	 * eg. "SELECT ... FROM ... WHERE <column selection> operator <column selection> operator ..." 
	 * @param s         Session object containing values
	 * @param operator  boolean SQL operator eg. "AND", "OR" etc
	 * @return
	 */
	public static String searchSql (SessionData s, String operator) {
		ArrayList<String> ss = new ArrayList<String>();
		for (DatabaseSession d : DatabaseSession.values()){
			ss.add(d.searchSql(s));
		}
		return StringUtils.join(ss.toArray(new String[ss.size()]), operator);
	}
	
	/**
	 * Generate WHERE expression SQL for single column, multiple values query
	 * eg. "SELECT ... FROM ... WHERE column=val1 OR column=val2 OR ..."
	 * @param values list of values for this column    
	 * @return
	 */
	public String searchSql (String[]values) {
		ArrayList<String> ss = new ArrayList<String>();
		for (int i = 0; i < values.length; i++) {
			ss.add(this.searchSql(values[i]));
		}
		return StringUtils.join(ss.toArray(new String[ss.size()]), " OR ");
	}
	
	/**
	 * This column's type
	 * @return
	 */
	public String getColumnDefinition () {
		if (!this.canBeNull) {
			return this.columnType.toString() + " NOT NULL";
		}
		else {
			return this.columnType.toString();
		}
	}
	
	
	public String updateSessionSql (String sess, String timestamp, String proj, Object newVal) {
		return "UPDATE " + PrearcDatabase.table + " SET " + 
		       this.searchSql(newVal) + " WHERE " +
		       DatabaseSession.sessionSql(sess, timestamp, proj);
	}
	
	public String findSql (String sess, String timestamp, String proj) {
		return "SELECT " + this.columnName + " FROM " + PrearcDatabase.table + " WHERE " + DatabaseSession.sessionSql(sess, timestamp, proj);
	}
	
	public String findSql (final Object o) {
		return "SELECT * FROM " + PrearcDatabase.table + " WHERE " + this.searchSql(o);
	}
	/**
	 * Generate SQL to find a row that matches all the given Sessions' slots.
	 * @param s
	 * @return
	 */
	public static String findSessionSql (SessionData s) {
		return "SELECT * FROM " + PrearcDatabase.table + " WHERE " + DatabaseSession.searchSql(s, " AND ");			
	}
	
	/**
	 * Generate SQL to find a row where that matches the given session and project
	 * @param sess
	 * @param proj
	 * @return
	 */
	public static String findSessionSql (String sess, String timestamp, String proj) {
		return "SELECT * FROM " + PrearcDatabase.table + " WHERE " + DatabaseSession.sessionSql(sess,timestamp,proj);
	}
		
	/**
	 * Count the number of sessions that match the given arguments.
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @return
	 */
	public static String countSessionSql (String sess, String timestamp, String proj) {
		String s = "SELECT COUNT(*) FROM " + PrearcDatabase.table + " WHERE " + DatabaseSession.sessionSql(sess, timestamp, proj);
		return s;
	}
	
	private static String sessionSql (String sess, String timestamp, String proj) {
		return DatabaseSession.FOLDER_NAME.searchSql(sess) + " AND " + 
               DatabaseSession.TIMESTAMP.searchSql(timestamp) + " AND " +
               DatabaseSession.PROJECT.searchSql(proj);
	}
	
	/**
	 * Generate SQL that deletes a row that matches the given session and project
	 * @param sess
	 * @param proj
	 * @return
	 */
	public static String deleteSessionSql (String sess, String timestamp, String proj) {
		return "DELETE FROM " + PrearcDatabase.table + " WHERE " + DatabaseSession.sessionSql(sess,timestamp,proj);
	}
	
	/**
	 * Generate SQL that queries any rows matching the given names.
	 * This query will fail if this column is not a string.
	 * @param names
	 * @return
	 */
	public String allMatchesSql (String[]names){
		return "SELECT * FROM " + PrearcDatabase.table + " WHERE " + this.searchSql(names);
	}
	
	/**
	 * Single-quote all strings in the array
	 * @param ss
	 * @return
	 */
	public static String[] singleQuote (String[]ss) {
		String[] _temp = new String[ss.length];			
		for (int i = 0; i < ss.length; i++) {
			_temp[i] = DatabaseSession.singleQuote(ss[i]);
		}
		return _temp;
	}
	
	/**
	 * Single quote the given object. 
	 * @param o
	 * @return
	 */
	public static String singleQuote(Object o) {
		return o != null ? "'" + o.toString() + "'" : "null";
	}

	/**
	 * Extract the value which corresponds to this column from the given session and insert it 
	 * into the given PreparedStatement.
	 * @param st
	 * @param s
	 * @throws SQLException
	 */
	public void setInsertStatement(PreparedStatement st, SessionData s) throws SQLException {
		this.columnType.setInsertStatement(this.ordinal()+1,st,this.readSession(s));
	}
	
	/**
	 * Extract this columns value from the ResultSet. 
	 * @param <T>
	 * @param r
	 * @return
	 * @throws SQLException
	 */
	public <T extends Object> T getFromResult(ResultSet r) throws SQLException {
		return (T) this.columnType.getFromResult(this.ordinal()+1,r);
	}
	
	/**
	 * Convert this columns value to a String 
	 * @param r
	 * @return
	 * @throws SQLException
	 */
	public String resultToString(ResultSet r) throws SQLException {
		return this.columnType.resultToString(this.ordinal() + 1, r);
	}
	
	/**
	 * Sql that creates the prearchive table.
	 * @return
	 */
	public static String createTableSql () {
		StringBuilder s = new StringBuilder();
		s.append("CREATE TABLE IF NOT EXISTS " + PrearcDatabase.table + "(");
		List<String> values = new ArrayList<String>();
		for (DatabaseSession d : DatabaseSession.values()) {
			values.add(d.getColumnName() + " " + d.getColumnDefinition());
		}
		s.append(StringUtils.join(values.toArray(), ','));
		s.append(")");
		return s.toString();
	}
	

	public static String getAllRows() {
		return "SELECT * FROM " + PrearcDatabase.table; 
	}
	
	/**
	 * Transfer the values from a ResultSet into a SessionData object 
	 * @param r
	 * @return
	 * @throws SQLException
	 */
	public static SessionData fillSession (ResultSet r) throws SQLException {
		SessionData s = new SessionData();
		for (DatabaseSession d : DatabaseSession.values()){
			d.writeSession(s, d.getFromResult(r));
		}
		return s;
	}
	
	/**
	 * Extract this columns value from the given Session.
	 * @param s
	 * @return
	 */
	public abstract Object readSession (SessionData s);
	
	/**
	 * Cast 'o' to the right type and map add to the appropriate slot in s.
	 * @param s
	 * @param o
	 */
	public abstract void writeSession (SessionData s, Object o);
}