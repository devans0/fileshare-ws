/**
 * title: File Info Structure
 * @author Dominic Evans
 * @date February 15, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * Data structure that contains information regarding a file that has been listed 
 * on the sharing service. Can be used for both search results and for enabling a
 * peer-to-peer connection to be established between consumers in order to begin
 * file transfer.
 * 
 * When a FileInfo object is returned as part of the search, the ownerIP and port
 * fields shall be null.
 */

package fileshare.ws.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	private int fileID; 
	private String fileName;
	private String ownerIP;
	private Integer port;
	
	public FileInfo() {} // ctor
	
	public FileInfo (int fileID, String fileName, String ownerIP, Integer port) {
		this.fileID = fileID;
		this.fileName = fileName;
		this.ownerIP = ownerIP;
		this.port = port;
	} // ctor

	/**
	 * @return the fileID
	 */
	public int getFileID() {
		return fileID;
	}

	/**
	 * @param fileID the fileID to set
	 */
	public void setFileID(int fileID) {
		this.fileID = fileID;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the ownerIP
	 */
	public String getOwnerIP() {
		return ownerIP;
	}

	/**
	 * @param ownerIP the ownerIP to set
	 */
	public void setOwnerIP(String ownerIP) {
		this.ownerIP = ownerIP;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

}