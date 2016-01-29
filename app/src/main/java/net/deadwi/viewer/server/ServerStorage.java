package net.deadwi.viewer.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by jihun.jo on 2016-01-26.
 */
public class ServerStorage {
    public static final int INDEX_NEW = -1;
    private static ServerStorage ourInstance = new ServerStorage();
    private String savePath;
    private ArrayList<ServerInfo> serverList;

    public static ServerStorage getInstance() {
        return ourInstance;
    }

    private ServerStorage() {
    }

    public void setSavePath(String path)
    {
        savePath = path;
        if(savePath.charAt(savePath.length()-1)!='/')
            savePath += "/";
        loadServerList();
    }

    private void loadServerList()
    {
        Log.d("SERVER","loadServerList");
        serverList = new ArrayList<ServerInfo>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(getSavePath()));
            String line;
            while ((line = br.readLine()) != null)
            {
                Log.d("SERVER","read : "+line);
                ServerInfo item = ServerInfo.getBookmarkItem(line);
                if(item==null)
                    continue;
                Log.d("SERVER","server : "+item.title);
                serverList.add(item);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveServerList()
    {
        try
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(getSavePath()));
            for(ServerInfo item : serverList)
            {
                bw.write(item.getLine());
            }
            bw.flush();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateServer(int index, ServerInfo item)
    {
        ServerInfo s = getServerInfo(index);
        if(s!=null)
            s.copyFrom(item);
        else
            serverList.add(item);
        saveServerList();
    }

    public int getServerCount()
    {
        return  serverList.size();
    }

    public ServerInfo getServerInfo(int index)
    {
        if(index>=0 && index<serverList.size())
            return serverList.get(index);
        return null;
    }

    private String getSavePath()
    {
        return savePath+"server.list";
    }
}


