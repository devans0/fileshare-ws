/**
 * title: File Share Web Service
 * @author Dominic Evans
 * @date February 15, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * This class supplies Web methods that a consumer can use to interface
 * with the DatabaseManager. Each method here simply wraps some functionality
 * provided by DatabaseManager. Consult that class for implementation details.
 */

package fileshare.ws.service;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;

import fileshare.ws.model.FileInfo;
import fileshare.ws.db.DatabaseManager;

@WebService(serviceName = "FileShareService")
public class FileShareWS {
	
	// Constructor required by JAX-WS
	public FileShareWS() {}
	
	@WebMethod
	public void listFile(String ownerID, String fileName, String ownerIP, int ownerPort) {
		DatabaseManager.listFile(ownerID, fileName, ownerIP, ownerPort);
	}
	
	@WebMethod
	public void delistFile(String fileName, String peerID) {
		DatabaseManager.delistFile(fileName, peerID);
	}
	
	@WebMethod
	public FileInfo[] searchFiles(String query) {
		return DatabaseManager.searchFiles(query);
	}
	
	@WebMethod
	public FileInfo getFileOwner(int fileID) {
		return DatabaseManager.getFileOwner(fileID);
	}
	
	@WebMethod
	public int getTTL() {
		return DatabaseManager.getTTL();
	}
	
	@WebMethod
	public boolean keepAlive(String clientID) {
		return DatabaseManager.keepAlive(clientID);
	}
	
	@WebMethod
	public void disconnect(String clientID) {
		DatabaseManager.disconnect(clientID);
	}
}
