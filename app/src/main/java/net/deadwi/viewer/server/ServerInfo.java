package net.deadwi.viewer.server;

/**
 * Created by jihun.jo on 2016-01-26.
 */
public class ServerInfo {
    public static final int METHOD_HTTP = 1000;

    public String title="";
    public int method;
    public String url="";
    public String user="";
    public String password="";

    static public ServerInfo getBookmarkItem(String line)
    {
        String[] tokens = line.split("\t");
        if(tokens.length<3)
            return null;
        ServerInfo item = new ServerInfo();
        item.title = tokens[0];
        item.method = Integer.parseInt(tokens[1]);
        item.url = tokens[2];
        if(tokens.length==5)
        {
            item.user = tokens[3];
            item.password = tokens[4];
        }
        return item;
    }

    public ServerInfo()
    {

    }

    public String getLine()
    {
        return title+"\t"+method+"\t"+url+"\t"+user+"\t"+password+"\n";
    }

    public void copyFrom(ServerInfo obj)
    {
        title = obj.title;
        method = obj.method;
        url = obj.url;
        user = obj.user;
        password = obj.password;
    }

    public String getUrlWithHttp()
    {
        if(url.startsWith("http://"))
            return url;
        return "http://"+url;
    }
}

