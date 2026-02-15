/**
 * title: Configuration Loader
 * description: Utility for parsing and returning values from ".properties" files
 * @author Dominic Evans
 * @date January 22 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans 
 */

package fileshare.ws.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
	private static Properties props = new Properties();
	
	/* 
	 * Initialize the ConfigLoader
	 * 
	 * getResourceAsStream necessary in order to properly integrate with Wildfly.
	 * Otherwise, the configuration file will be searched for in the working directory
	 * of the execution which will be the Wildfly home directory. This ensures
	 * that the configuration file can be found.
	 * 
	 * This requires that the configuration file be found in src/main/resources so
	 * that it is properly placed where Wildfly may find it.
	 */
	static {
		try (InputStream is = Thread.currentThread().getContextClassLoader()
								.getResourceAsStream("fileservice.properties")) {
			
			if (is != null) {
				props.load(is);
			} else {
				System.err.println("[WARN] ConfigLoader: fileservice.properties NOT FOUND in classpath");
			}
		} catch (IOException ioe) {
			System.err.println("[ERR] ConfigLoader could not find fileservice.properties");
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Returns the value associated with the provided key
	 * @param key indicating which property to access
	 * @return String value associated with the desired property
	 */
	public static String getProperty(String key) {
		return props.getProperty(key);
	} // getProperty
	
	/* Overload: provide an optional default configuration value */
	public static String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	} // getProperty
	
	/**
	 * Fetches the integer value associated with a given key
	 * @param key indicating which property to access; must be associated with an
	 * integer-valued property field
	 * @return Integer value associated with the provided key
	 */
	public static int getIntProperty(String key) {
		String resultStr = props.getProperty(key);
		if (resultStr == null) {
			throw new RuntimeException("Missing required configuration key: " + key);
		}
		return Integer.parseInt(resultStr);
	} // getIntProperty
	
	/* Overload: provide an optional default configuration value */
	public static int getIntProperty(String key, int defaultVal) {
		String resultStr = props.getProperty(key);
		if (resultStr == null) {
			return defaultVal;
		} 
		return Integer.parseInt(resultStr);
	} // getIntProperty
	
	/**
	 * Fetches the boolean value associated with a given key and returns its value
	 * @param key indicating the property to be returned from the configuration file.
	 * @return boolean corresponding to desired key
	 */
	public static boolean getBooleanProperty(String key) {
		return Boolean.parseBoolean(props.getProperty(key));
	} // getBooleanProperty  
	
	/* Overload: provide an optional default configuration value */
	public static boolean getBooleanProperty(String key, boolean defaultValue) {
		String val = props.getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		return Boolean.parseBoolean(val);
	} // getBooleanProperty
}
