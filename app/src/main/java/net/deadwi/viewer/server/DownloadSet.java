package net.deadwi.viewer.server;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by jihun.jo on 2016-02-01.
 */
public class DownloadSet
{
    public Deque<DownloadFile> downloadQue;
    public DownloadFile downloading;

    public DownloadSet()
    {
        downloadQue = new ArrayDeque<>(50);
        downloading = null;
    }

}
