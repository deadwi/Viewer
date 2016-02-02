package net.deadwi.viewer.server;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import net.deadwi.viewer.FileItem;
import net.deadwi.viewer.FileManager;
import net.deadwi.viewer.Option;
import net.deadwi.viewer.server.ServerListActivity;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Random;
import java.util.Timer;

/**
 * Created by jihun.jo on 2016-01-27.
 */
public class HTTPSeverConnector
{
    private Handler handler;
    private String serverURL; // for list
    private String serverUser; // for list
    private String serverPassword; // for list
    private String downloadPath;
    //private volatile ArrayList<String[]> lastList;
    private volatile ListResult lastList;
    private DownloadSet downloadSet;
    private DownloadListTask listTask=null;
    private DownloadFileThread fileThread=null;

    private class DownloadListTask extends AsyncTask<String, Integer, ListResult> {
        public DownloadListTask()
        {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ListResult doInBackground(String... paths)
        {
            if(paths.length!=4)
                return null;
            String url = paths[0];
            String path = paths[1];
            String user = paths[2];
            String password = paths[3];
            return HTTPSeverConnector.getListResult(url, path, user, password);
        }

        @Override
        protected void onPostExecute(ListResult result) {
            // 작업 완료 (취소 제외)
            lastList = result;

            Message msg = Message.obtain();
            Bundle data = new Bundle();

            if(result.errorMessage==null)
            {
                msg.what = ServerListActivity.EVENT_UPDATE_LIST_OK;
                data.putString(ServerListActivity.MSG_DATA_PATH, result.path);
            }
            else
            {
                msg.what = ServerListActivity.EVENT_UPDATE_LIST_FAIL;
                data.putString(ServerListActivity.MSG_DATA_MESSAGE, result.errorMessage);
            }
            msg.setData(data);
            handler.sendMessage(msg);
        }

        @Override
        protected void onCancelled(ListResult result) {
            // 취소 결과값
            super.onCancelled(null);
        }
    }

    private class DownloadFileThread extends Thread  {
        @Override
        public void run()
        {
            int bufferSize = 1024*4;
            byte[] buffer = new byte[bufferSize];

            while(!Thread.currentThread().isInterrupted())
            {
                DownloadFile df;
                synchronized (downloadSet)
                {
                    df = downloadSet.downloadQue.pollFirst();
                    downloadSet.downloading = df;
                    if(df==null)
                    {
                        try {
                            downloadSet.wait();
                        } catch (InterruptedException e)
                        {
                        }
                        continue;
                    }
                }
                if(df.isDirectory)
                {
                    // add files in directory
                    ListResult lr =  HTTPSeverConnector.getListResult(df.serverInfo.getUrlWithHttp(), df.fullPath, df.serverInfo.user, df.serverInfo.password);
                    synchronized (downloadSet)
                    {
                        for(int i=lr.files.size()-1;i>=0;i--)
                        {
                            FileItem item = lr.files.get(i);
                            downloadSet.downloadQue.addFirst(new DownloadFile(df.serverInfo,
                                    item.getFullPath(),
                                    FileManager.getFullPath(df.target, item.name),
                                    item.size,
                                    item.type == FileItem.TYPE_DIR) );
                        }
                        downloadSet.downloading = null;
                    }
                    continue;
                }

                try
                {
                    Log.d("HTTP", "download start : " + df.target);
                    String fullPath = FileManager.getFullPath(df.serverInfo.getUrlWithHttp(),df.fullPath);
                    URLConnection conn = getURLConnection(fullPath, df.serverInfo.user, df.serverInfo.password);
                    BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                    BufferedOutputStream bout = new BufferedOutputStream(getFileOutputStream(df));

                    int readSize = 0;
                    while (true)
                    {
                        if(isCancelled() == true)
                        {
                            Log.d("HTTP", "download cancel : " + df.target);
                            break;
                        }
                        // blocking
                        readSize = bis.read(buffer);
                        //Log.d("HTTP", "get : " + readSize);
                        if (readSize > 0)
                            bout.write(buffer, 0, readSize);
                        else if (readSize < 0)
                            break;
                    }
                    bout.close();
                    Log.d("HTTP", "download end : " + df.target);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                finally
                {
                    // download complete (or error)
                    synchronized (downloadSet)
                    {
                        downloadSet.downloading=null;
                    }
                    Message msg = Message.obtain();
                    msg.what = ServerListActivity.EVENT_UPDATE_DOWNLOAD_LIST;
                    handler.sendMessage(msg);
                }
            }
        }

        private FileOutputStream getFileOutputStream(DownloadFile df) throws IOException
        {
            File file = FileManager.getFileWithDirectory(FileManager.getFullPath(Option.getInstance().getDownloadPath(),df.target));
            if(file==null)
                return null;
            return new FileOutputStream(file);
        }

        private boolean isCancelled()
        {
            boolean isnull = false;
            synchronized (downloadSet)
            {
                isnull = downloadSet.downloading==null;
            }
            return isnull;
        }
    }

    static public String getURLString(String fullPath) throws IOException, URISyntaxException
    {
        URL url = new URL(fullPath);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        return uri.toASCIIString();
    }

    static public URLConnection getURLConnection(String fullPath,String user,String password) throws IOException, URISyntaxException
    {
        URL updateURL = new URL( getURLString(fullPath) );
        URLConnection conn = updateURL.openConnection();
        if(user!=null && password!=null && user.isEmpty()==false && password.isEmpty()==false)
        {
            String login = user + ":" + password;
            String base64login = Base64.encodeToString(login.getBytes(), Base64.NO_WRAP); // no newline option
            conn.setRequestProperty("Authorization", "Basic " + base64login);
        }
        return conn;
    }

    static public Connection getJsoupConnection(String fullPath,String user,String password) throws IOException, URISyntaxException {
        String url = getURLString(fullPath);
        Log.d("HTTP", "request : " + fullPath);
        Connection connect = Jsoup.connect(url);
        if(user!=null && password!=null && user.isEmpty()==false && password.isEmpty()==false)
        {
            String login = user + ":" + password;
            String base64login = Base64.encodeToString(login.getBytes(), Base64.NO_WRAP); // no newline option
            connect.header("Authorization", "Basic " + base64login);
        }
        return connect;
    }

    static public ListResult getListResult(String url, String path, String user, String password)
    {
        String fullPath = FileManager.getFullPath(url, path);
        // for comicglass server
        if(fullPath.endsWith("/")==false)
            fullPath += "/";

        ListResult lr = new ListResult(path);
        try
        {
            Document document = getJsoupConnection(fullPath, user, password).get();
            if(getListResultFromTR(document,lr)==false)
                getListResultFromLink(document, lr);
        }
        catch (HttpStatusException ex)
        {
            if(ex.getStatusCode()==401)
                lr.errorMessage = "Unauthorized";
            else
                lr.errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
        catch (Exception ex)
        {
            lr.errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
        return lr;
    }

    static public boolean getListResultFromTR(Document document,ListResult lr) throws Exception
    {
        Elements trs = document.getElementsByTag("tr");
        if(trs.isEmpty())
            return false;

        int indexName=-1;
        int indexDate=-1;
        int indexSize=-1;
        boolean isFirstRow = true;
        for(Element tr : trs)
        {
            Elements tds = tr.children(); // td, th
            if(tds==null || tds.size()==0)
                continue;

            if(isFirstRow)
            {
                for(int i=0;i<tds.size();i++)
                {
                    String f = tds.get(i).text().trim().toLowerCase();
                    if(f.compareTo("name")==0)
                        indexName = i;
                    else if(f.compareTo("last modified")==0)
                        indexDate = i;
                    else if(f.compareTo("size")==0)
                        indexSize = i;
                }
                isFirstRow = false;
                if(indexName==-1)
                {
                    lr.errorMessage = "Invalid File index page";
                    break;
                }
            }
            else
            {
                if(indexName>=tds.size())
                    continue;

                String name = tds.get(indexName).text();
                if(name.compareTo("/")==0 || name.trim().toLowerCase().startsWith("parent directory"))
                    continue;

                int type = FileItem.TYPE_FILE;
                long size = indexSize < tds.size() ? getFileSize(tds.get(indexSize).text()) : 0;
                if(name.endsWith("/"))
                {
                    type = FileItem.TYPE_DIR;
                    name = name.substring(0, name.length()-1);
                }
                lr.files.add(new FileItem(lr.path, name,
                        indexDate < tds.size() ? tds.get(indexDate).text() : "",
                        type,
                        size));
            }
        }
        return true;
    }

    static public boolean getListResultFromLink(Document document,ListResult lr) throws Exception
    {
        Elements links = document.getElementsByTag("a");
        if(links.isEmpty())
            return false;

        for(Element link : links)
        {
            String path = link.attr("href");
            String title = link.attr("booktitle");
            String date = link.attr("bookdate");
            String size = link.attr("booksize");
            if(path==null || title==null)
                continue;
            path = URLDecoder.decode(path, "utf-8");

            int type = FileItem.TYPE_FILE;
            if(path.endsWith("/"))
            {
                type = FileItem.TYPE_DIR;
                path = path.substring(0, path.length()-1);
            }
            lr.files.add(new FileItem(lr.path,
                    path,
                    date==null ? "" : date,
                    type,
                    getFileSize(size)));
            /*
            lr.files.add(new FileItem(FileManager.getFullPath(lr.path,FileManager.getPathFromFullpath(path, path)),
                    title,
                    date==null ? "" : date,
                    type,
                    getFileSize(size)));
                    */
        }
        return true;
    }

    static private long getFileSize(String value)
    {
        double size=0;
        if(value==null || value.length()==0)
            return 0;

        long unit=1;
        char last = value.charAt(value.length() - 1);
        if(Character.isDigit(last)==false)
        {
            switch (last)
            {
                case 'K':
                    unit=1024;
                    break;
                case 'M':
                    unit=1024*1024;
                    break;
                case 'G':
                    unit=1024*1024*1024;
                    break;
                default:
                    unit=0;
                    break;
            }
            value = value.substring(0, value.length()-1);
        }

        try
        {
            size =  Double.parseDouble(value);
        }
        catch (Exception e)
        {
        }
        return (long)(size*unit);
    }

    public HTTPSeverConnector(Handler _handler, DownloadSet _downloadSet)
    {
        handler = _handler;
        downloadSet = _downloadSet;
        fileThread = new DownloadFileThread();
        fileThread.start();
    }

    public void connect(String url, String user, String password)
    {
        serverURL = url;
        serverUser = user;
        serverPassword = password;
    }

    public void request(String path)
    {
        if(listTask!=null && listTask.getStatus()!= AsyncTask.Status.FINISHED)
        {
            listTask.cancel(true);
            listTask = null;
        }
        listTask = new DownloadListTask();
        listTask.execute(serverURL,path,serverUser,serverPassword);
    }

    public ArrayList<FileItem> getFileList()
    {
        if(lastList==null)
            return null;
        return lastList.files;
    }
}

class ListResult
{
    String path;
    String errorMessage;
    ArrayList<FileItem> files;

    ListResult(String _path)
    {
        path = _path;
        errorMessage = null;
        files = new ArrayList<>();
    }
}

