/**
 * title: Database Manager
 * @author Dominic Evans
 * @date February 15, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * Manages a PostgreSQL database server in order to provide the functionality required
 * by the file sharing service. Provides methods that are used to update the database
 * with a new file that is being shared, removing files from the share database, 
 */

package fileshare.ws.db;

import fileshare.ws.model.FileInfo;
import fileshare.ws.util.ConfigLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
	// Defaults
	private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/catalog_db";
	private static final String DEFAULT_DB_USER = "fileshare_service";
	private static final String DEFAULT_DB_PASS = "";

	// Database configuration
	private static String dbURL;
	private static String dbUser;
	private static String dbPassword;

	// Indicates whether the database is confirmed initialized
	private static boolean initialized = false;
	
	// Guard against instantiation
	private DatabaseManager() {}

	/*
	 * Static initializer ensures that the database is in good working order before
	 * any transactions are executed against it. Ensures that the required table
	 * exists in the database and that the database is responding to queries.
	 */
	static {
		dbURL = ConfigLoader.getProperty("db.url", DEFAULT_DB_URL);
		dbUser = ConfigLoader.getProperty("db.user", DEFAULT_DB_USER);
		dbPassword = ConfigLoader.getProperty("db.password", DEFAULT_DB_PASS);
		initialized = true;
		verifyDatabase();
	}

	/**
	 * Returns a java.sql.Conneciton to the database described by the properties
	 * file used to initialize the DatabaseConfig object.
	 * 
	 * @return java.sql.Connection object to the PostgreSQL server described by the
	 *         properties file
	 * @throws SQLException in the case of the PostgreSQL driver not being found or
	 *                      when a connection is requested from an uninitialized
	 *                      DatabaseConfig object.
	 */
	private static Connection getConnection() throws SQLException {
		if (!initialized) {
			throw new SQLException("DatabaseConfig has not been initialized with a properties file.");
		}
		try {
			// Explicitly load the Driver to force an exception if it is not present; this
			// enables
			// a more descriptive exception to be thrown
			Class.forName("org.postgresql.Driver");
			return DriverManager.getConnection(dbURL, dbUser, dbPassword);
		} catch (ClassNotFoundException cnf) {
			throw new SQLException("PostgreSQL Driver not found in classpath.", cnf);
		}
	} // getConnection

	/**
	 * Verifies that the database exists and that it has the appropriate schema for
	 * use. If the database cannot be reached, this constitutes a critical error and
	 * results in a runtime exception.
	 */
	private static void verifyDatabase() {
		try (Connection conn = getConnection()) {

			DatabaseMetaData dbm = conn.getMetaData();
			try (ResultSet tables = dbm.getTables(null, null, "file_entries", null)) {
				if (!tables.next()) {
					System.out.println("[DB] Table 'file_entries' not found. Running setup...");
					runSetupScript(conn);
				} else {
					System.out.println("[DB] Database schema verified.");
				}
			}
		} catch (SQLException sqle) {
			throw new RuntimeException("Critical DB Error", sqle);
		}
	}

	/**
	 * verifyDatabase() helper. Runs the setup.sql script to initialize the table
	 * schema required by the server.
	 * 
	 * @param conn A connection to the database server establishes prior to method
	 *             call.
	 * @throws SQLException If the setup script is not found then this method cannot
	 *                      complete its setup tasks.
	 */
	private static void runSetupScript(Connection conn) throws SQLException {
		try (InputStream is = Thread.currentThread().getContextClassLoader()
								.getResourceAsStream("setup.sql")){

			if (is == null) {
				throw new IOException("setup.sql not found on classpath");
			}

			String sqlInit = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sqlInit);
			}
		} catch (IOException ioe) {
			System.err.println("[DB] Could not load setup.sql " + ioe.getMessage());
			throw new SQLException("Initialization script missing or unreadable.", ioe);
		}
	} // runSetupScript

	/**
	 * Reaps stale files from the database
	 * 
	 * @param staleFileAge the maximum age that a file can be before it is
	 *                     considered stale. Interpreted as seconds.
	 * @return int number of files that the reaper has removed from the database.
	 */
	public static int reap(String staleFileAge) {
		if (staleFileAge == null) {
			return 0;
		}
		
		int staleAgeSecs;
		try {
			staleAgeSecs = Integer.parseInt(staleFileAge);
		} catch (NumberFormatException nfe) {
			throw new RuntimeException("[REAPER] stale_file_age invalid, check configuration", nfe);
		}
		
		String sql = "DELETE FROM file_entries WHERE last_seen < NOW() - INTERVAL '" + 
					  (staleAgeSecs + 1) + " seconds'";
		int deletedRows = 0;

		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			deletedRows = pstmt.executeUpdate(sql);
			if (deletedRows > 0) {
				System.out.println("[REAPER] Purged " + deletedRows + " stale file(s).");
			}
		} catch (SQLException sqle) {
			System.err.println("[REAPER] Error during cleanup " + sqle.getMessage());
		}
		return deletedRows;
	} // reap

	/**
	 * Makes a file available in the directory database for other clients to
	 * discover.
	 * 
	 * @param ownerID   UUID of the client; generated client side and used to
	 *                  uniquely identify a client on subsequent accesses to the
	 *                  same file listing.
	 * @param fileName  the name of the file.
	 * @param ownerIP   the IP address of the owner of the file.
	 * @param ownerPort the port that the owner of the file is accepting connection
	 *                  on.
	 */
	public static void listFile(String ownerID, String fileName, String ownerIP, int ownerPort) {
		String sql = "INSERT INTO file_entries (peer_id, file_name, owner_ip, owner_port, last_seen) " + 
					 "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " + 
				     "ON CONFLICT (file_name, owner_ip, owner_port) " + 
					 "DO UPDATE SET last_seen = CURRENT_TIMESTAMP";

		try (Connection conn = getConnection(); 
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, ownerID);
			pstmt.setString(2, fileName);
			pstmt.setString(3, ownerIP);
			pstmt.setInt(4, ownerPort);
			pstmt.executeUpdate();

			System.out.println("[DB] Listing updated: " + fileName + " from " + ownerIP);
		} catch (SQLException sqle) {
			System.err.println("[DB] Error in listFile: " + sqle.getMessage());
		}
	} // listFile

	/**
	 * Allows the owner of a file to remove it from the file tracking database.
	 * 
	 * @param fileName the name of the file that is to be removed.
	 * @param peerID the UUID of the client that owns that file; used as proof of ownership
	 */
	public static void delistFile(String fileName, String peerID) {
		String sql = "DELETE FROM file_entries WHERE file_name = ? AND peer_id = ?";

		try (Connection conn = getConnection(); 
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			// Construct the prepared statement and execute it against the database
			pstmt.setString(1, fileName);
			pstmt.setString(2, peerID);
			int rows = pstmt.executeUpdate();

			// Report results to stdout
			if (rows == 0) {
				System.out.println("[AUTH] Unauthorized delist attempt for: " + fileName);
			} else {
				System.out.println("[DB] delisting file '" + fileName + "'");
			}
		} catch (SQLException sqle) {
			System.err.println("[DB] Error in delistFile: " + sqle.getMessage());
		}
	} // delistFile

	/**
	 * Searches the database for any listings that match a provided file name. Each matching file is 
	 * returned with its ID and name in an array.
	 * 
	 * @param query the search query entered by the user.
	 * @return FileInfo[] array of file info objects corresponding to the search results.
	 */
	public static FileInfo[] searchFiles(String query) {
		String sql = "SELECT id, file_name FROM file_entries " + "WHERE file_name ILIKE ?";

		List<FileInfo> fileList = new ArrayList<>();

		try (Connection conn = getConnection(); 
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, "%" + query + "%");

			// Iterate over the results and add new FileInfo objects to the list
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					fileList.add(new FileInfo(rs.getInt("id"), rs.getString("file_name"), null, null));
				}
			}
		} catch (SQLException sqle) {
			System.err.println("[DB] Error in searchFiles: " + sqle.getMessage());
		}

		// CORBA expects an array type per the IDL interface specification
		return fileList.toArray(new FileInfo[0]);
	} // searchFiles

	/**
	 * Finds the detailed information regarding a specific file listed with the
	 * service.
	 * 
	 * @param fileID the ID of the file of interest; this was obtained from a
	 *               previous search.
	 * @return FileInfo detailed information about the file in question; enough
	 *         information to instigate a peer connection for sharing the file.
	 */
	public static FileInfo getFileOwner(int fileID) {
		// This search should benefit from indexing; searchFiles() is not used to avoid
		// duplicating the searches when gathering data vs. getting particular file info
		String sql = "SELECT file_name, owner_ip, owner_port FROM file_entries " + 
					 "WHERE id = ? LIMIT 1";

		try (Connection conn = getConnection(); 
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			// Construct the prepared statement and execute it
			pstmt.setInt(1, fileID);
			try (ResultSet rs = pstmt.executeQuery()) {
				// There is only one row or no rows in the ResultSet
				if (rs.next()) {
					// Package the database return into a FileInfo object and return it
					return new FileInfo(fileID, rs.getString("file_name"), rs.getString("owner_ip"),
							rs.getInt("owner_port"));
				}
			}
		} catch (SQLException sqle) {
			System.err.println("[DB] Error in getFileOwner: " + sqle.getMessage());
		}

		// No exact match was found for the file
		return null;
	} // getFileOwner

	/**
	 * Reports the maximum time that a file has to live in the absence of a
	 * heartbeat signal.
	 * 
	 * @return int the number of seconds that a file will exist in the database
	 *         before it becomes stale.
	 */
	public static int getTTL() {
		return ConfigLoader.getIntProperty("db.stale_file_age", 30);
	} // getTTL

	/**
	 * Enables updating the last_seen attribute of a file that is listed in the
	 * catalog server. This stops the file from becoming stale and being removed by
	 * the reaper. The client is identified through the use of a UUID which is used
	 * to update all files that that client has listed with the server.
	 * 
	 * The boolean return for this function aims to signal to a client that calls it
	 * that the update actually performed an update in the database. If it returns
	 * false, this means that the client does not actually have any files listed in
	 * the database. This is used to signal that the client must refresh their
	 * shared files with the server, as they have all been reaped or de-listed in
	 * some other way.
	 * 
	 * @param clientID   UUID of the client sending the heartbeat
	 * @return true if the signal updated any records in the database; false
	 *         otherwise, indicating that the client does not have any files listed
	 *         on the server
	 */
	
	public static boolean keepAlive(String clientID) {
		String sql = "UPDATE file_entries SET last_seen = CURRENT_TIMESTAMP " +
					 "WHERE peer_id = ?";
		
		int rows = 0;
		try (Connection conn = getConnection(); 
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			
			// Construct the prepared statement and execute it against the database
			pstmt.setString(1, clientID);
			rows = pstmt.executeUpdate();
			
			// Report any heartbeats that do not correspond to a client UUID
			if (rows == 0) {
				System.out.println("[AUTH] Failed heartbeat for ID: " + clientID);
			}
		} catch (SQLException sqle) {
			System.err.println("[DB] Error in keepAlive: " + sqle.getMessage());
		}
		return rows > 0;
	} // keepAlive

	/**
	 * Disconnects a client which removes all files listed by that client. After
	 * sending this signal a client shall immediately cease listing any files with
	 * the server.
	 * 
	 * @param clientID identifier UUID for the client which is signaling
	 *                 disconnection.
	 */
	public static void disconnect(String clientID) {
		String sql = "DELETE FROM file_entries WHERE peer_id = ?";

		try (Connection conn = getConnection(); 
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, clientID);
			pstmt.execute();

		} catch (SQLException sqle) {
			System.err.println("[DB] Error in disconnect: Database operation failed.");
			sqle.printStackTrace();
		}
	} // disconnect
}