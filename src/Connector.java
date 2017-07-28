import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Connector<T> {

	/**
	 * List all files whose name contain prefix
	 */
	public List<T> list(String prefix) throws Exception;

	public InputStream get(String key) throws Exception;
	
	/**
	 * Get the file from drive and save it to the local file 
	 * with type specified
	 */
	public Long get(String key, File tempFile, String type) throws Exception;
	
	/**
	 * Create a file in drive with content from a local file
	 */
	public String put(File localFile) throws IOException;

	/**
	 * Update content of a file in drive
	 */
	public String update(String key, java.io.File localFile) throws IOException;

	/**
	 * Delete a file in drive
	 */
	public void delete(String key) throws Exception;
	
	public Long getGDocs(String key, java.io.File tempFile, String type) throws IOException;
}
