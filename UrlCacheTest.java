import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.*;
 
public class UrlCacheTest {
   private UrlCache cache;
 
   @Before
   public void setUp() throws Exception {
      cache = new UrlCache();
   }
 
   @After
   public void tearDown() throws Exception {
   }
   
   @Test
   public void testGetObject() throws UrlCacheException {
	   cache.getObject("people.ucalgary.ca/~mghaderi/index.html");
	   boolean urlSuccess = cache.urlList.contains("people.ucalgary.ca/~mghaderi/index.html");
	   boolean dateSuccess = cache.dateList.contains("Thu, 18 Sep 2014 22:24:31 GMT");
	   assertTrue("error in getObject", urlSuccess);
	   assertTrue("error in getObject", dateSuccess);
   }
   
   @Test
   public void testGetLastModified() throws UrlCacheException {
	   cache.getObject("people.ucalgary.ca/~mghaderi/index.html");
	   long expected = 1411079071000L;
	   long date = cache.getLastModified("people.ucalgary.ca/~mghaderi/index.html");
	   assertEquals("error in getLastModified", expected, date);
   }
   
   @Test
   public void testCreateRequest() {
	   String path = "~mghaderi/index.html";
	   String host = "people.ucalgary.ca";
	   long time = 1411079071000L;
	   String expected = "GET /" + path + " HTTP/1.1\r\nHost: " + host + "\r\n" +
						"If-Modified-Since: Thu, 18 Sep 2014 04:24:31 MDT\r\n";
	   String actual = cache.createRequestString(host, path, time);
	   System.out.println(actual);
	   assertEquals("error in createRequestString", expected, actual);
   }
   
   @Test
   public void testCreateFile() throws IOException {
	   String path = "test/test";
	   File f = new File(path);
	   cache.createNewFile(path);
	   boolean success = f.exists();
	   assertTrue("error in createNewFile", success);
   }
   
   @Test
   public void testAddToCatalog() throws IOException {
	   String url = "people.ucalgary.ca/~mghaderi/index.html";
	   String date = "Thu, 18 Sep 2014 04:24:31 MDT";
	   cache.addToCatalog(url, date);
	   boolean urlSuccess = cache.urlList.contains(url);
	   boolean dateSuccess = cache.dateList.contains(date);
	   assertTrue("error in getObject", urlSuccess);
	   assertTrue("error in getObject", dateSuccess);
   }
   
   @Test
   public void testInitCatalog() throws Exception {
	   cache.initCatalog();
	   File f = new File("catalog");
	   boolean success = f.exists();
	   assertTrue("error in createNewFile", success);
   }
   
   @Test
   public void testParseUrl() throws UrlCacheException {
	   UrlCache.Url u = cache.parseUrl("people.ucalgary.ca/~mghaderi/index.html");
	   assertEquals("error in parseUrl", "people.ucalgary.ca", u.host);
	   assertEquals("error in parseUrl", "~mghaderi/index.html", u.path);
	   assertEquals("error in parseUrl", 80, u.port);
   }
 
   @Test(expected = UrlCacheException.class)
   public void testLastModifiedInvalidUrl() throws UrlCacheException {
      cache.getLastModified("invalid");
   }
   
   @Test(expected = UrlCacheException.class)
   public void testGetObjectInvalidUrl() throws UrlCacheException {
      cache.getObject("invalid");
   }
}