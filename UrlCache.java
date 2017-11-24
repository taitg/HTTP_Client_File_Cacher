
import java.net.Socket;
import java.io.*;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

/**
 * UrlCache Class
 * 
 * @author 	Geordie Tait
 * @version	1.1, Oct 14, 2016
 *
 */
public class UrlCache {
    public ArrayList<String> urlList;
    public ArrayList<String> dateList;
    private static String datePattern = "EEE, dd MMM yyyy hh:mm:ss zzz";

    /**
    * UrlCache Class
    * 
    * Object for storing host, path and port parsed from a URL
    */
    public class Url {
    	String host;
    	String path;
    	int port;
    }

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 *
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public UrlCache() throws UrlCacheException {
		// ArrayLists for containing the catalog
        urlList = new ArrayList<String>();
        dateList = new ArrayList<String>();

        try {
        	// initialize the catalog
            initCatalog();
	    }
        catch(Exception e) {
            throw new UrlCacheException("Error initializing UrlCache - " + e.getMessage());
        }
    }

    /**
     * Creates the catalog file if it doesn't exist, or loads the contained values into the arrays
	 *
     * @throws IOException If there is an error creating or accessing the catalog file
     * @throws FileNotFoundException	If the catalog file cannot be found
     */
	public void initCatalog() throws IOException, FileNotFoundException {
		File catalogf = new File("catalog");
		
		// create the catalog file if there isn't one
		if (!catalogf.exists()) catalogf.createNewFile();
		else {
			// load the values from catalog file
		    FileInputStream catalogfis = new FileInputStream("catalog");
		    Scanner s = new Scanner(catalogfis);

		    while (s.hasNextLine()) {
		        urlList.add(s.nextLine());
		        dateList.add(s.nextLine());
		    }

		    catalogfis.close();
		    s.close();
		}
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public void getObject(String url) throws UrlCacheException {
        // parse the URL into host, path, port 
		Url u = parseUrl(url);

        try {
        	// initialize the TCP socket
            Socket sock = new Socket(u.host, u.port);
            
            // initialize data streams
            InputStream in = sock.getInputStream();
            PrintWriter out = new PrintWriter(new DataOutputStream(sock.getOutputStream()));
            ByteArrayOutputStream headBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
            byte[] bytes = new byte[16384];
            int n = -1;

            // get last modified time if it's in the catalog, else 0
            long lastModMilli = 0;
            if ((urlList != null) && urlList.contains(url)) lastModMilli = getLastModified(url);

            // create and send an HTTP request from the given URL info
            out.println(createRequestString(u.host, u.path, lastModMilli));
            out.flush();

            // read the header and file data sent back from the server
            int headerEndIndex;
            boolean readHeader = true;
            while ((n = in.read(bytes)) != -1) {
                if (readHeader) {
                    headBytes.write(bytes, 0, n);
                    headerEndIndex = headBytes.toString().lastIndexOf("\r\n\r\n")+4;
                    readHeader = false;
                    fileBytes.write(bytes, headerEndIndex, n - headerEndIndex);
                }
                else fileBytes.write(bytes, 0, n);
            }

            // from the header parse the last modified time sent from the server
            int lastModIndex = headBytes.toString().indexOf("Last-Modified:") + 15;
            String date = headBytes.toString().substring(lastModIndex, lastModIndex+29);

            // create the new file and necessary directory structure
            createNewFile(u.path);

            // write to the new file
            FileOutputStream fout = new FileOutputStream(u.path);
            fout.write(fileBytes.toByteArray());

            // add the URL and last modified date to the catalog
            addToCatalog(url, date);
            
            // close data streams and socket
            fout.close();
            in.close();
            out.close();
            sock.close();
        }
        catch(Exception e) {
            throw new UrlCacheException("Error getting object - " + e.getMessage());
        }
	}

    /**
     * Parses the given URL into host, path and port and returns them as a URL object
	 *
     * @param url 	URL of the object
	 * @return The URL object containing the host, path, and port
     * @throws UrlCacheException	If the URL is not valid
     */
	public Url parseUrl(String url) throws UrlCacheException {
		// create a new Url object
		Url u = new Url();
		
		// parse the path name, throw an exception if there isn't one
        String[] parts1 = url.split("/", 2);
        if (parts1.length > 1) u.path = parts1[1];
        else throw new UrlCacheException("Invalid URL");

        // parse the host name
        String[] parts2 = parts1[0].split(":");
        u.host = parts2[0];

        // parse the port if it exists, else use 80
        if (parts2.length > 1) u.port = Integer.parseInt(parts2[1]);
        else u.port = 80;
        
        // return the Url object
        return u;
	}

    /**
     * Adds the specified URL and last modified time to the catalog file and arrays
	 *
     * @param url 	URL of the object
     * @param date	The last modified time of the object
     * @throws IOException	If there is an error accessing the catalog file
     */
	public void addToCatalog(String url, String date) throws IOException {
		// write the URL and last modified date (one line each) to catalog file
		FileWriter catalogfw = new FileWriter("catalog", true);
		catalogfw.write(url + "\n" + date + "\n");

		// add the url and date to the arrays
		urlList.add(url);
		dateList.add(date);

		catalogfw.close();
	}

    /**
     * Creates the file specified in path and any necessary directory structure
	 *
     * @param path	The path of the file to create
     * @throws IOException	If there is an error creating the file or directories 
     */
	public void createNewFile(String path) throws IOException {
		File f = new File(path);
		f.getParentFile().mkdirs();	// create any necessary directory structure
		f.createNewFile();			// create the file (if it doesn't exist)
	}

    /**
     * Returns a conditional GET HTTP request as a string
	 *
     * @param host	The hostname as a string
     * @param path	The pathname as a string
     * @param lastModMilli	The last modified time in millisecond format 
	 * @return the HTTP request as a string
     */
	public String createRequestString(String host, String path, long lastModMilli) {
		SimpleDateFormat format = new SimpleDateFormat(datePattern);
		Date lastModDate = new Date(lastModMilli);

		return "GET /" + path + " HTTP/1.1\r\nHost: " + host + "\r\n" +
				"If-Modified-Since: " + format.format(lastModDate) + "\r\n";
	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     * @throws UrlCacheException if the specified url is not in the cache, or there are other errors/exceptions
     */
	public long getLastModified(String url) throws UrlCacheException {
        try {
            SimpleDateFormat format = new SimpleDateFormat(datePattern);
            
            // get, convert, and return the last modified date associated with given URL
            int index = urlList.indexOf(url);
            String dateString = dateList.get(index);
            Date date = format.parse(dateString);
            return date.getTime();
        }
        catch(Exception e) {
            throw new UrlCacheException("Error getting last modified date - " + e.getMessage());
        }
	}
}
