package net.deadwi.viewer.server;

import android.os.Handler;
import android.util.Log;

import net.deadwi.viewer.FileItem;
import net.deadwi.viewer.FileManager;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by jihun.jo on 2016-01-26.
 */
public class ServerManager
{
    private int sortType = FileManager.SORT_ALPHA_WITH_NUM;
    private ServerInfo currentServerInfo;
    private HTTPSeverConnector serverHttp;
    private Handler handler;
    private Deque<DownloadFile> downloadQue;

    public ServerManager(Handler _handler)
    {
        handler = _handler;
        downloadQue = new ArrayDeque<>(50);
        serverHttp = new HTTPSeverConnector(handler, downloadQue);
    }

    public boolean requestSetDir(String path)
    {
        if(path!=null)
        {
            serverHttp.request(path);
            return true;
        }
        return false;
    }

    public boolean movePreviousDir(String path)
    {
        if(path==null)
            return false;
        // root
        if(path.compareTo("/")==0 || path.isEmpty())
            return requestSetDir(null);
        return requestSetDir(FileManager.getPathFromFullpath(path, ""));
    }

    public boolean moveNextDir(String path, String name)
    {
        return requestSetDir(FileManager.getFullPath(path, name));
    }

    public String getCurrentName(String path)
    {
        if(path==null)
            return "Server List";
        return FileManager.getNameFromFullpath(path);
    }

    public void connectServer(String path)
    {
        currentServerInfo = ServerStorage.getInstance().getServerInfo( Integer.parseInt(path) );
        String username = "foo";
        String password = "bar";
        String login = username + ":" + password;
        //String base64login = new String(Base64.encodeBase64(login.getBytes()));
        //.header("Authorization", "Basic " + base64login)
        //serverHttp.get("http://d.redduck.com/img/");
        serverHttp.connect(currentServerInfo.getUrlWithHttp(), currentServerInfo.user, currentServerInfo.password);
        requestSetDir("");
    }

    public void disconnectServer()
    {
    }

    public ArrayList<FileItem> getCurrentFiles(String path)
    {
        Log.d("SERVER", (path==null ? "root" : path) );
        ArrayList<FileItem> fileList=null;
        if(path==null)
            fileList = getServerList();
        else
        {
            fileList = getFileList();
            FileManager.sortFilelist(fileList, sortType);
        }
        return fileList;
    }

    public ArrayList<FileItem> getMatchFiles(String path,String keyword)
    {
        Log.d("SERVER", (path==null ? "root" : path) );;
        ArrayList<FileItem> fileList=null;
        if(path==null)
            fileList = getServerList();
        else
        {
            fileList = getFileList();
            FileManager.filterFilelist(fileList, keyword);
            FileManager.sortFilelist(fileList, sortType);
        }
        return fileList;
    }

    public void addDownload(String fullPath, String name, boolean isDirectory)
    {
        synchronized (downloadQue)
        {
            downloadQue.addLast(new DownloadFile(currentServerInfo, fullPath, name, isDirectory));
            downloadQue.notify();
        }
    }

    private ArrayList<FileItem> getServerList()
    {
        ArrayList<FileItem> fileList = new ArrayList<>();
        int count = ServerStorage.getInstance().getServerCount();
        for(int i=0;i<count;i++)
        {
            ServerInfo info = ServerStorage.getInstance().getServerInfo(i);
            fileList.add(new FileItem(""+i, info.title, FileItem.TYPE_SITE, 0));
        }

        fileList.add(new FileItem("","New Server", FileItem.TYPE_NEW_SITE,0 ));
        return fileList;
    }

    private ArrayList<FileItem> getFileList()
    {
        return serverHttp.getFileList();
    }
}
