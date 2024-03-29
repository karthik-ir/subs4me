package utils;

import java.awt.Component;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.HasParentFilter;
import org.htmlparser.filters.HasSiblingFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.json.JSONArray;
import org.json.JSONObject;

import subs.Subs4me;


public class Utils
{
    private static final String TEMP_SUBS_ZIPPED_FILE = "temp";
    private static final String HTTP_REFERER = "http://www.tvrage.com/";

    public static HttpURLConnection createPost(String urlString,
            StringBuffer extraProps)
    {
        URL url;
        HttpURLConnection connection = null;
        PrintWriter out;

        try
        {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);

            // more or less of these may be required
            // see Request Header Definitions:
            // http://www.ietf.org/rfc/rfc2616.txt
            connection.setRequestProperty("Accept-Charset", "*");
            connection.setRequestProperty("Accept_Languaget", "en-us,en;q=0.5");
            connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
            connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.setRequestProperty("Referer", "www.torec.net");
           

            out = new PrintWriter(connection.getOutputStream());
            out.print(extraProps);
            out.close();
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return connection;
    }

    /**
     * unzip the files
     * @param zipName 
     * 
     * @param retainTheSameName
     * 
     * @param src
     * @param fileNoExt
     * @param retainTheSameName
     *            - true will keep the original filename as there is only one
     *            download, false will change to filename + entry.srt
     * @param url url reporting is now possible in the unzip
     */
    public static void unzipSubs(FileStruct currentFile,
            String zipName, boolean retainTheSameName, String url)
    {
        Enumeration entries;
        ZipFile zipFile;
        String property = "java.io.tmpdir";
        String tempDir = System.getProperty(property);
        try
        {
            zipFile = new ZipFile(tempDir + zipName);
            entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (entry.isDirectory())
                {
                    // Assume directories are stored parents first then
                    // children.
                    // System.err.println("Extracting directory: "
                    // + entry.getName());
                    // This is not robust, just for demonstration purposes.
                    (new File(entry.getName())).mkdir();
                    continue;
                }

                // System.err.println("Extracting file: " + entry.getName());
                String destFileName = "";
                if (entry.getName().endsWith(".srt"))
                {
                    if (retainTheSameName)
                    {
                        destFileName = currentFile.buildDestSrt();
                    }
                    else
                    {
                        destFileName = currentFile.buildDestSrt() + "."
                                + entry.getName();
                    }
                    // destFileName = currentFile.getSrcDir() + File.separator
                    // + currentFile.getNameNoExt() + ".srt";
                    
                    System.out.println("downloaded:" + url + ", originally called:" + entry.getName());
                }
                else
                {
                    destFileName = currentFile.getSrcDir() + File.separator
                            + entry.getName();
                }
                copyInputStream(zipFile.getInputStream(entry),
                        new BufferedOutputStream(new FileOutputStream(
                                destFileName)));
            }
            zipFile.close();
            File del = new File(tempDir + zipName);
            Utils.deleteFile(del);
        }
        catch (IOException ioe)
        {
            // System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out)
            throws IOException
    {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
        out.close();
    }

    /**
     * 
     * @param file
     * @param fileName 
     * @return succeeded true or false
     */
    public static boolean downloadZippedSubs(String file, String fileName)
    {
        return downloadZippedSubs(file, fileName, null);
    }
    /**
     * 
     * @param file
     * @param fileName 
     * @param cookie
     * @return succeeded true or false
     */
    public static boolean downloadZippedSubs(String file, String fileName, String cookie)
    {
        StringBuffer sb = new StringBuffer(file);
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        String property = "java.io.tmpdir";
        String tempDir = System.getProperty(property);
        
        URL url;
        try
        {
            File tmpDir = new File(tempDir);
            if (!tmpDir.exists());
            {
                tmpDir.mkdirs();
            }
            File destination = null;
            destination = new File(tempDir + fileName);
            url = new URL(sb.toString());
            URLConnection urlc = url.openConnection();
            if (cookie != null)
            {
                urlc.setRequestProperty("Cookie", cookie);
            }
            
            bis = new BufferedInputStream(urlc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(destination));

            int i;
            while ((i = bis.read()) != -1)
            {
                bos.write(i);
            }
            if (bis != null)
                try
                {
                    bis.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            if (bos != null)
                try
                {
                    bos.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            return true;
        }
        catch (MalformedURLException e1)
        {
            return false;
            // System.out.println("could not download subs: " +
            // e1.getMessage());
        }
        catch (IOException e)
        {
            return false;
            // System.out.println("could not download subs: " + e.getMessage());
        }
        finally
        {
        }
        // return false;
    }

    public static String escape(String string)
    {
        return string.replaceAll(" ", "%20");
    }
    
    public static List<String> unEscape(String string, boolean replaceVerbus)
    {
        List<String> lst = new LinkedList<String>();
        String rep = string;
        //strip " &#x22;
        rep = rep.replaceAll("&#x22;", "");
        rep = rep.replaceAll("\"", "");
        rep = rep.trim();
        
        String repx = rep.replaceAll("&#x26;", "and");
        lst.add(repx);
        repx = rep.replaceAll("&#x26;", "&amp;");
        lst.add(repx);
        repx = repx.replaceAll("&amp;", "and");
        lst.add(repx);
        return lst;
    }

    public static boolean isInRange(String num, String range)
    {
        if (range == null)
            return false;
        
        if (num.equals(range))
        {
            return true;
        }

        if (range.indexOf("-") > -1)
        {
            int start = Integer.parseInt(range.substring(0, range.indexOf("-"))
                    .trim());
            int end = Integer.parseInt(range.substring(range.indexOf("-") + 1)
                    .trim());
            for (int i = start; i < end; i++)
            {
                if (num.equals(Integer.toString(i)))
                    return true;
            }
            return false;
        }

        return false;
    }

//    public static boolean compareHDLevel(String n1, FileStruct f1)
//    {
//        Pattern p1 = Pattern.compile(FileStruct.hdLevel);
//        Matcher m1 = p1.matcher(n1);
//        String fileHd = null;
//        if (m1.find())
//        {
//            fileHd = m1.group();
//        }
//        if (fileHd != null)
//            fileHd = fileHd.replaceAll("[pP]", "");
//
//        if (fileHd == f1.getHDLevel())
//        {
//            return true;
//        }
//
//        if ((fileHd == null && f1.getHDLevel() != null)
//                || (fileHd != null && f1.getHDLevel() == null))
//        {
//            return false;
//        }
//
//        if (fileHd.equalsIgnoreCase(f1.getHDLevel()))
//            return true;
//
//        return false;
//    }

    public static boolean compareReleaseSources(String n1, FileStruct f1)
    {
        Pattern p1 = Pattern.compile(FileStruct.releaseSourcePattern);
        Matcher m1 = p1.matcher(n1);
        String fileSource = null;
        if (m1.find())
        {
            fileSource = m1.group();
        }

        if (fileSource == f1.getSource())
        {
            return true;
        }

        if ((fileSource == null && f1.getSource() != null)
                || (fileSource != null && f1.getSource() == null))
        {
            return false;
        }

        if (fileSource.equalsIgnoreCase(f1.getSource()))
            return true;

        return false;
    }

    public static String parseReleaseName(String name)
    {
        String group = "(-\\w*(-)|($))|(-\\w*$)|(\\A\\w*-)";
        Pattern p1 = Pattern.compile(group);
        Matcher m1 = p1.matcher(name);
        if (m1.find())
        {
            return m1.group().replaceAll("-", "");
        }

        return "";
    }

    public static boolean isSameMovie(FileStruct ff1, FileStruct ff2)
    {
        if (isReleaseAMatch(ff1.getReleaseName(),ff2.getReleaseName()))
        {
            if (ff1.getHD() != null && ff2.getHD() != null
                    && ff1.getHD().equalsIgnoreCase(ff2.getHD()))
            {
                return true;
            }
        }
        String file1 = ff1.getFullFileNameNoGroup();
        String file2 = ff2.getFullFileNameNoGroup();
        
        if (file1.equals(file2))
            return true;

        String f1 = file1.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        String f2 = file2.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        if (f1.equalsIgnoreCase(f2))
            return true;

        return false;
    }
    
    /**
     * compare the release names
     * @param rel1
     * @param rel2 always considered to be the real file, and not the name from the site
     * @return
     */
    public static boolean isReleaseAMatch(String rel1, String rel2)
    {
        if (rel1.equalsIgnoreCase(rel2))
            return true;
        
        if (rel1.toLowerCase().startsWith(rel2.toLowerCase()))
            return true;
        
        return false;
    }
    
    /**
     * 
     * @param ff1
     * @param ff2
     * @return always considered to be the real file, and not the name from the site
     */
    public static boolean isSameMovie2(String ff1, String ff2)
    {
        if (ff1.equals(ff2))
            return true;

        String f1 = ff1.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        String f2 = ff2.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "");
        if (f1.equalsIgnoreCase(f2))
            return true;

        return false;
    }

    /**
     * check if a movie starts with the same name
     * @param ff1
     * @param ff2
     * @return
     */
    public static boolean isSameMovie3(String ff1, String original)
    {
        if (ff1.equals(original))
            return true;

        String f1 = ff1.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "").toLowerCase();
        String f2 = original.replaceAll("[.]", "").replaceAll(" ", "").replaceAll("-", "").toLowerCase();
        if (f1.startsWith(f2))
            return true;

        return false;
    }
    
 // Get the contents of a URL and return it as an image
    public static Image fetchimage(String address, Component c)
            throws MalformedURLException, IOException
    {
        URL url = new URL(address);
        return c.createImage((java.awt.image.ImageProducer) url.getContent());
    }
    
    public static String searchRealTVShowNameUsingGoogle(String fileName)
    {
        return searchRealNameUsingGoogle(fileName, "www.tvrage.com");
    }
    
    public static String searchRealMovieNameUsingGoogle(String fileName)
    {
        return searchRealNameUsingGoogle(fileName, "www.imdb.com");
    }
    
    /**
     * try to locate a page where the file name is mentioned and an imdb site url is also present
     * @param q 
     * @return
     */
    public static String searchRealNameUsingGoogle(String fileName, String searchForCritiria)
    {
        String query = fileName + " " + searchForCritiria; 
        System.out.println("Querying Google for " + query);
        try
        {
            // Convert spaces to +, etc. to make a valid URL
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL(
                    "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q="
                            + query);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", HTTP_REFERER);

            // Get the JSON response
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            while ((line = reader.readLine()) != null)
            {
                builder.append(line);
            }

            String response = builder.toString();
            JSONObject json = new JSONObject(response);

            JSONArray ja = json.getJSONObject("responseData").getJSONArray("results");

//            System.out.println("Total results = "
//                    + json.getJSONObject("responseData")
//                            .getJSONObject("cursor").getString(
//                                    "estimatedResultCount"));

//            JSONArray ja = json.getJSONObject("responseData").getJSONArray(
//                    "results");

//            System.out.println(" Results:");
            if (ja != null && ja.length() > 0)
            {
                for (int i = 0; i < ja.length(); i++)
                {
                    JSONObject j = ja.getJSONObject(i);
                    String urlTemp = j.getString("url");
                    urlTemp = URLDecoder.decode(urlTemp, "UTF-8");
                    urlTemp = checkURLOk(urlTemp);
                    if ( urlTemp != null)
                    {
                        return urlTemp;
                    }
                }
               
            }
//            for (int i = 0; i < ja.length(); i++)
//            {
//                System.out.print((i + 1) + ". ");
//                JSONObject j = ja.getJSONObject(i);
//                System.out.println(j.getString("titleNoFormatting"));
//                System.out.println(j.getString("url"));
//            }
        } catch (Exception e)
        {
            System.err.println("Something went wrong...");
            e.printStackTrace();
        }
        return null;
    }
    
    public static String createGoogleQuery(String query) 
    {
         // Convert spaces to +, etc. to make a valid URL
            try
            {
                query = URLEncoder.encode(query, "UTF-8");
                URL url = new URL(
                        "http://www.google.com/search?hl=en&source=hp&q="
                        + query+ "&btnG=Google+Search&aq=f&aqi=&oq=");
                return url.toString();
            } catch (UnsupportedEncodingException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MalformedURLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return null;
    }
    
  //scrape google
    public static String searchRealNameUsingGoogle2(String fileName, String searchForCritiria)
    {
        String query = "\"" + fileName + "\" " + searchForCritiria + " download"; 
        System.out.println("Querying Google for " + query);
        Parser parser;
        try
        {
         // Convert spaces to +, etc. to make a valid URL
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL(
                    "http://www.google.com/search?hl=en&source=hp&q="
                            + query+ "&btnG=Google+Search&aq=f&aqi=&oq=");
            parser = new Parser(url.toString());
            parser.setEncoding("UTF-8");
            NodeFilter filter = new AndFilter(
                    new TagNameFilter("div"), new HasAttributeFilter("class", "s"));
//            NodeFilter filter = new RegexFilter("www.imdb.com/title</em>/tt[\\d]*");
            NodeList list = new NodeList();
            list = parser.parse(filter);
//            for (NodeIterator e = parser.elements(); e.hasMoreNodes();)
//            {
//                Node node =  e.nextNode();
////                System.out.println(node.toHtml());
//                node.collectInto(list, filter);
//            }
            Pattern p = Pattern.compile("http://www.imdb.com/[ ]*title/[a-z]{2}[0-9]+");
            Node[] nodes = list.toNodeArray();
            for (int i = 0; i < nodes.length; i++)
            {
                Node nd = nodes[i];
                
                Matcher m = p.matcher(nd.toPlainTextString());
                if (m.find())
                {
//                    String me = m.group();
//                    System.out.println("******** " + m.group());
                    String urll = m.group();
                    urll = urll.replaceAll(" ", "");
                    return(urll);
                }
//                System.out.println(nd.toPlainTextString());
            }
        } catch (ParserException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static String checkURLOk(String url)
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url); 
     // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        try
        {
            String responseBody = httpclient.execute(httpget, responseHandler);
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() == 200
                    && responseBody.indexOf("http://www.imdb.com/title") >-1)
            {
//                int i = responseBody.indexOf("http://www.imdb.com/title");
                Pattern p = Pattern.compile("http://www.imdb.com/title/[a-z,0-9]*");
                Matcher m = p.matcher(responseBody);
                if (m.find())
                {
                    return(m.group());
                }
            }
            
            // When HttpClient instance is no longer needed, 
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();        
        } 
        catch (ClientProtocolException e)
        {
        } 
        catch (IOException e)
        {
        }
        
        return null;
    }
    
    public static List<String> locateRealNameUsingGoogle(String fullName)
    {
        List<String> names = locateRealNameUsingGoogle(fullName, "www.imdb.com/title");
        if (names == null)
        {
            names = locateRealNameUsingGoogle(fullName, "www.tv.com");
        }
        return names;
    }

    /**
     * find out the real name of the tvshow/movie
     * @param fullName
     * @return
     */
    public static List<String> locateRealNameUsingGoogle(String fullName, String critiria)
    {
      //try to find the real name of the movie using google
        String imdbUrl = Utils.searchRealNameUsingGoogle2(fullName, critiria);
        if (imdbUrl != null)
        {
            try
            {
                Parser parser = new Parser(imdbUrl);
                parser.setEncoding("UTF-8");
                NodeFilter parentFilter = new AndFilter(
                        new TagNameFilter("div"), new HasAttributeFilter("id", "tn15title"));
                NodeFilter filter = new AndFilter(new TagNameFilter("h1"), 
                        new HasParentFilter(parentFilter));
                
                NodeList list = parser.parse(filter);
                String tmpName = ((HeadingTag)list.toNodeArray()[0]).getChild(0).toPlainTextString();
//                tmpName = tmpName.replaceAll("\\([\\d]*\\)$", "");
                List<String> lst = new LinkedList<String>();
                List<String> namesLst =  unEscape(tmpName, true);
                addToList(lst, namesLst);

                parser = new Parser(imdbUrl + "/releaseinfo#akas");
                parser.setEncoding("UTF-8");
                NodeFilter filter2 = new AndFilter(new TagNameFilter("a"), new HasAttributeFilter("name", "akas"));
                list = new NodeList();
                list = parser.parse(filter2);
                if (list != null && list.size() >0)
                {
                    Node table = list.elementAt(0).getParent().getNextSibling().getNextSibling();
                    Pattern p1 = Pattern.compile("<td>(.*)</td>");
                    Matcher m = p1.matcher(table.toHtml());
                    int i = 0;
                    while (m.find())
                    {
                        if (i % 2 > 0)
                        {
                            i++;
                            continue;
                        }
                        
                        String gr = m.group(1);
                        List<String> namesLst1 =  unEscape(gr, true);
                        addToList(lst, namesLst1);
                        i++;
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (String name : lst)
                {
                    sb.append(name);
                    sb.append(", ");
                }
                System.out.println("*** Google says - Movie real name is:" + sb.toString());
                return lst;
                
            } catch (ParserException e1)
            {
                System.out.println("***** Error trying to get file name using google for: " + fullName);
            }
        }
        else
        {
            System.out.println("***** Google did not find a result for: " + fullName);
        }
        
        return null;
    }
    
    private static void addToList(List<String> lst, List<String> newNames)
    {
        for (String name : newNames)
        {
            boolean found = false;
            for (String alreadyInNmaes : lst)
            {
                if (alreadyInNmaes.equals(name))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                lst.add(name);
            }
        }
    }

    public static boolean isMovieFile(FileStruct file)
    {
        if (file.getExt().equalsIgnoreCase("mkv")
                || file.getExt().equalsIgnoreCase("avi"))
        {
            return true;
        }
        
        return false;
    }
    
    final static int BUFF_SIZE = 100000;
    final static byte[] buffer = new byte[BUFF_SIZE];
    public static int copy(InputStream is, OutputStream os) throws IOException {
        int bytesCopied = 0;
        try {
            while (true) {
                synchronized (buffer) {
                    int amountRead = is.read(buffer);
                    if (amountRead == -1) {
                        break;
                    }
                    else {
                        bytesCopied += amountRead;
                    }
                    os.write(buffer, 0, amountRead);
                }
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException error) {
                // ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException error) {
                // ignore
            }
        }
        return bytesCopied;
    }

    public static void writeDoWorkFile(FileStruct currentFile, StringBuilder data)
    {
        File f = new File(currentFile.getFile().getParent(), currentFile.getFullNameNoExt() + Subs4me.DO_WORK_EXT);
        try
        {
            String session = "";
            if (f.exists())
            {
                FileReader reader = new FileReader(f);
                BufferedReader in = new BufferedReader(reader);
//                Scanner s = new sc
                //first line must be session
                session = in.readLine();
                in.close();
            }
            if (session != null && !session.isEmpty())
            {
                Pattern p = Pattern.compile("session = ([\\d]*)");
                Matcher m = p.matcher(session);
                if (m.find())
                    session =  m.group(1);
                else
                    session = "";
                
                if (!session.equals(Subs4me.SESSION_ID))
                    session = "";
                
            }
            if (session == null || session.isEmpty())
            {
                FileWriter dowrkFile = new FileWriter(f);
                session = "session = " + Subs4me.SESSION_ID;
                dowrkFile.write(session + "\n");
                dowrkFile.write(data.toString());
                dowrkFile.close();
            }
            else
            {
                FileReader reader = new FileReader(f);
                BufferedReader in = new BufferedReader(reader);
                String line = null;
                StringBuilder sb = new StringBuilder();
                while ((line = in.readLine()) != null)
                {
                    sb.append(line);
                    sb.append(System.getProperty("line.separator"));
                }
                //first line must be session
//                session = in.readLine();
                in.close();
                FileWriter dowrkFile = new FileWriter(f);
                dowrkFile.write(sb.toString());
                dowrkFile.append(data.toString());
                dowrkFile.close();
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * check if file is not a video file, and if it is less 100K, obly then delete it
     * @param f file to delete
     */
    public static void deleteFile(File f)
    {
        String ext = null;
        Matcher m1 = FileStruct.EXT_PATTERN.matcher(f.getName());
        if (m1.find())
        {
            ext = m1.group(1);
        }
        if (ext != null
                && !ext.equals(".mkv")
                && !ext.equals(".avi")
                && f.length() < 100000
                )
        {
            f.delete();
        }
    }
    
}
