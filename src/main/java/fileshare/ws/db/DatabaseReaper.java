/**
 * title: Database Reaper
 * @author Dominic Evans
 * @date February 15, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * Runs when the web services is deployed in order to clean up file share database
 * entries that have become stale. 
 * 
 * A file is stale if it has not received a heartbeat update from its owner in a 
 * configured interval of time, measured in seconds. The amount of time that needs 
 * to pass before a file is considered stale is configured in fileservice.properties
 * and defaults to 30 seconds.
 */

package fileshare.ws.db;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import fileshare.ws.util.ConfigLoader;

@Singleton 
@Startup
public class DatabaseReaper {
	private final String STALE_FILE_AGE;
	
	// Initialize the reaper by defining the maximum age of a file before it is considered stale
	public DatabaseReaper() {
		STALE_FILE_AGE = ConfigLoader.getProperty("db.stale_file_age", "30");
	}
	
	// Run a cleanup operation every 60 seconds
	@Schedule(second="0", minute = "*", hour = "*", persistent = false)
	public void executeCleanup() {
		int deletedRows = DatabaseManager.reap(STALE_FILE_AGE);
		if (deletedRows > 0) {
			System.out.println("[REAPER] Removed " + deletedRows + " stale file entries.");
		}
	}
}