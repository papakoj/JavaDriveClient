import com.google.api.client.util.DateTime;

public class MyFile {
	
	private static String name;
	private static DateTime lastModified;
	
	public MyFile(String name, DateTime lastModified) {
		this.name = name;
		this.lastModified = lastModified;
	}
	
	public static String getName() {
		return name;
	}
	
	public static DateTime getLastModified() {
		return lastModified;
	}
	
	public String toString() {
		return name + ": " + lastModified.toString();
	}
}
