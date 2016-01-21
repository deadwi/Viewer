package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import net.deadwi.library.FreeImageWrapper;

/**
 * Created by jihun.jo on 2016-01-15.
 */
@SuppressLint("ViewConstructor")
class FastImageView extends DoubleBufferView
{
    private String zipPath;

    public FastImageView(Context context, int width, int height)
    {
        super(context, width, height);
        zipPath = null;
    }

    public void setZipPath(String _zipPath)
    {
        zipPath = _zipPath;
    }

    protected int drawImageFromPathToBitmap(String path, Bitmap bitmap, boolean isLastPage, int viewIndex)
    {
        if (zipPath == null)
            return FreeImageWrapper.loadImageFromPath(bitmap, path, isLastPage, viewIndex, getOptionViewMode(), optionResizeMode(), optionResizeMethod(), optionFilter());
        else
        {
            String innerPath = path;
            if(innerPath.charAt(0)=='/')
                innerPath = innerPath.substring(1);
            return FreeImageWrapper.loadImageFromZip(bitmap, zipPath, innerPath, isLastPage, viewIndex, getOptionViewMode(), optionResizeMode(), optionResizeMethod(), optionFilter());
        }
    }
}

class FastImageViewPageController implements FastViewPageController
{
    private FastImageView fastView;
    private String path;
    private String zipPath;
    private String[] files;
    private int currntFileIndex;

    public FastImageViewPageController(FastImageView _fastView, String _path, String _zipPath, String[] _files)
    {
        fastView = _fastView;
        path = _path;
        zipPath = _zipPath;
        files = _files;

        fastView.setZipPath(zipPath);
        currntFileIndex = getStartPageIndex();
    }

    public int getCurrentPageIndex()
    {
        return currntFileIndex;
    }

    public void setCurrentPageIndex(int i)
    {
        currntFileIndex = i;
    }

    public String getTitle()
    {
        if(zipPath!=null)
            return FileManager.getNameWithoutExt(zipPath);
        return FileManager.getNameWithoutExt( FileManager.getPathFromFullpath(path,"/root") );
    }

    public int getPageCount()
    {
        return files.length;
    }

    public void saveBookmark(int pageIndex, int viewIndex)
    {
        if(zipPath!=null)
        {
            String dir = FileManager.getPathFromFullpath(zipPath, "/");
            BookmarkItem item = new BookmarkItem();
            item.filename = FileManager.getNameFromFullpath(zipPath);
            item.innerName = (pageIndex>=0 && pageIndex <= files.length - 1) ? files[pageIndex] : "";
            item.fileIndex = pageIndex<0 ? 0 : pageIndex;
            item.fileCount = files.length;
            item.viewIndex = viewIndex;
            Bookmark.getInstance().updateBookmark(dir, item);
        }
        saveLastView(null,null,0);
    }

    public void requestPage(int fileIndex, int viewIndex, boolean isPrev)
    {
        if(fileIndex<0 || fileIndex>=files.length)
            return;
        String path = files[fileIndex];
        String prepareFilePath = fileIndex + 1 < files.length ? files[fileIndex + 1] : null;

        Log.d("PAGE", "Request Page : file(" + (fileIndex + 1) + "/" + files.length + ") view("
                + (viewIndex + 1) + "/" + fastView.getAllViewCount()
                + (isPrev ? " LAST" : "")+")");
        fastView.drawImage(path, isPrev, viewIndex, prepareFilePath);

        saveLastView(zipPath, path, viewIndex);
    }

    public boolean requestNextPage()
    {
        if (currntFileIndex>=0 && currntFileIndex < getPageCount() - 1)
        {
            currntFileIndex++;
            requestPage(currntFileIndex, 0, false);
            return true;
        }
        return false;
    }

    public boolean requestPreviousPage()
    {
        if (currntFileIndex> 0)
        {
            currntFileIndex--;
            requestPage(currntFileIndex, 0, true);
            return true;
        }
        return false;
    }

    private int getStartPageIndex()
    {
        int index = 0;
        if(path!=null)
            index = getFileIndex(path);
        if(index<0)
            index = 0;
        return index;
    }

    private int getFileIndex(String path)
    {
        int index = -1;
        for(int i=0;i<files.length;i++)
        {
            if(files[i]==null)
                continue;

            if(path.compareTo(files[i])==0)
            {
                index = i;
                break;
            }
        }
        return index;
    }

    private void saveLastView(String lastZipPath, String lastPath, int lastViewIndex)
    {
        Option.getInstance().setLastView(lastZipPath, lastPath, lastViewIndex);
        Option.getInstance().saveLastView();
    }
}
