package net.deadwi.viewer.server;

/**
 * Created by jihun.jo on 2016-01-28.
 */
public class DownloadFile
{
    public ServerInfo serverInfo;
    // full path in server
    public String fullPath;
    // name with path (making path)
    public String target;
    public long size;
    public long downloadSize;
    public boolean isDirectory;

    public DownloadFile(ServerInfo _serverInfo,String _fullPath, String _target, long _size, boolean _isDirectory)
    {
        serverInfo = _serverInfo;
        fullPath = _fullPath;
        target = _target;
        size = _size;
        downloadSize = 0;
        isDirectory = _isDirectory;
    }
}
