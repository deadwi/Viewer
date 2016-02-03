package net.deadwi.viewer.server;

import android.os.Handler;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by jihun.jo on 2016-02-01.
 */
public class DownloadSet
{
    public Deque<DownloadFile> downloadQue;
    public DownloadFile downloading;
    public Handler handler;

    public DownloadSet()
    {
        downloadQue = new ArrayDeque<>(50);
        downloading = null;
        handler = null;
    }

}
