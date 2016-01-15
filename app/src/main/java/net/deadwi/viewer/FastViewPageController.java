package net.deadwi.viewer;

/**
 * Created by jihun.jo on 2016-01-15.
 */
public interface FastViewPageController
{
    public int getStartPageIndex();
    public String getTitle();
    public int getPageCount();
    public void saveBookmark(int pageIndex, int viewIndex);
    public void requestPage(int fileIndex, int viewIndex, boolean isPrev);
}

