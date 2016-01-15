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
class FastImageView extends FastView implements Runnable
{
    private boolean isBitmap1Out = true;
    private Object lock = new Object();
    private Bitmap mBitmap1;
    private Bitmap mBitmap2;

    private boolean isThreadRun = false;
    private Thread loaderThread;

    private boolean currentDraw = false;
    private FastImageViewLocation current;
    private FastImageViewLocation next;

    public void startBackgroundLoader()
    {
        isThreadRun = true;
        loaderThread = new Thread(this);
        loaderThread.start();
    }

    public void stopBackgroundLoader()
    {
        isThreadRun = false;
        synchronized(lock)
        {
            lock.notify();
        }
    }

    @Override
    public void run()
    {
        int ret;
        while(isThreadRun)
        {
            synchronized(lock)
            {
                Log.d("FASTIMAGE2", "Loop");
                lock.notify();
                try
                {
                    if(current.complete==false || currentDraw==false || next.complete==true)
                        lock.wait();
                }
                catch (InterruptedException e)
                {
                }

                if(current.complete==false)
                {
                    if(current.path!=null)
                    {
                        Log.d("FASTIMAGE2", "Current loading : "+current.path+" viewIndex : "+current.viewIndex + (current.isLastPage ? "(LAST)" : ""));
                        if (current.zipPath == null)
                            ret = drawImageFromPathToBitmap(current.path, getOutBitmap(), current.isLastPage, current.viewIndex);
                        else
                            ret = drawImageFromZipPathToBitmap(current.zipPath, current.path, getOutBitmap(), current.isLastPage, current.viewIndex);
                        if(ret>=0)
                        {
                            current.isDoublePage = ret>= FreeImageWrapper.RETURN_PAGE_UNIT;
                            current.viewCount = ret % FreeImageWrapper.RETURN_PAGE_UNIT;
                            if(current.isLastPage)
                            {
                                if(current.isDoublePage && current.viewIndex<current.viewCount)
                                    current.viewIndex += current.viewCount;
                                current.isLastPage = false;
                            }
                            Log.d("FASTIMAGE2", "Current loaded : all view count="+current.getAllViewCount());

                            // has next view
                            if(current.hasNextView())
                            {
                                if(next.isReady(current.path,current.getNextViewIndex())==false)
                                {
                                    next.copyFrom(current);
                                    next.complete = false;
                                    next.viewIndex = next.getNextViewIndex();
                                }
                            }
                        }
                        else
                        {
                            Log.e("FASTIMAGE2", "Current loading fail(code="+ret+")");
                        }
                    }
                    current.complete = true;
                    postInvalidate();
                }
                else if(next.complete==false && currentDraw==true)
                {
                    if(next.path!=null)
                    {
                        Log.d("FASTIMAGE2", "Next loading : "+next.path+" viewIndex : "+next.viewIndex);
                        if(next.zipPath==null)
                            ret = drawImageFromPathToBitmap(next.path, getSubBitmap(), false, next.viewIndex);
                        else
                            ret = drawImageFromZipPathToBitmap(next.zipPath, next.path, getSubBitmap(), false, next.viewIndex);
                        if(ret>=0)
                        {
                            next.isDoublePage = ret >= FreeImageWrapper.RETURN_PAGE_UNIT;
                            next.viewCount = ret % FreeImageWrapper.RETURN_PAGE_UNIT;
                            Log.d("FASTIMAGE2", "Next loaded : all view count="+next.getAllViewCount());
                        }
                        else
                        {
                            Log.e("FASTIMAGE2", "Next loading fail(code="+ret+")");
                        }
                    }
                    next.complete = true;
                }
            }
        }
    }

    public FastImageView(Context context, int width, int height)
    {
        super(context);
        if(mBitmap1!=null && mBitmap1.getWidth()!=width)
        {
            mBitmap1.recycle();
            mBitmap2.recycle();
            mBitmap1=null;
            mBitmap2=null;
        }

        if(mBitmap1==null)
        {
            mBitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            mBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }

        current = new FastImageViewLocation();
        next = new FastImageViewLocation();
    }

    public void clearImage()
    {
        synchronized(lock)
        {
            current.complete = false;
            next.complete = false;
        }
    }

    public int getViewIndex()
    {
        return current.viewIndex;
    }

    public int getAllViewCount()
    {
        return current.getAllViewCount();
    }

    public boolean hasNextView()
    {
        return current.hasNextView();
    }

    public boolean hasPrevView()
    {
        return current.hasPrevView();
    }

    public int getNextViewIndex()
    {
        return current.getNextViewIndex();
    }

    public int getPrevViewIndex(boolean top)
    {
        return current.getPrevViewIndex(top);
    }

    public void drawImageFromZipPath(String zipPath, String path, boolean isLastPage, int viewIndex, String nextPath)
    {
        synchronized(lock)
        {
            if(current.complete==false && next.complete==false)
            {
                // wait;

            }

            FastImageViewLocation tcurrent = current.clone();
            int nextViewIndex=0;

            currentDraw = false;
            // next request
            // request page is ready
            if(next.isReady(path,viewIndex))
            {
                current.copyFrom(next);

                Log.d("FASTIMAGE2", "Current page prepared");
                isBitmap1Out = !isBitmap1Out;
                postInvalidate();
            }
            else
            {
                current.complete = false;
                current.path = path;
                current.zipPath = zipPath;
                current.viewIndex = viewIndex;
                current.isLastPage = isLastPage;
                current.isDoublePage = false;
                current.viewCount = viewIndex+1;

                if(tcurrent.isSameFile(path)==true)
                {
                    current.isDoublePage = tcurrent.isDoublePage;
                    current.viewCount = tcurrent.viewCount;
                }
            }

            next.complete = false;
            // has next view
            if(current.hasNextView())
            {
                nextPath = current.path;
                nextViewIndex = current.getNextViewIndex();
            }
            if(nextPath!=null)
            {
                Log.d("FASTIMAGE2", "Next page view : "+nextViewIndex);

                // prev request
                // next of request's  page is ready
                if(tcurrent.isReady(nextPath,nextViewIndex))
                {
                    next.copyFrom(tcurrent);

                    Log.d("FASTIMAGE2", "Next page prepared");
                    isBitmap1Out = !isBitmap1Out;
                }
                else
                {
                    next.path = nextPath;
                    next.zipPath = zipPath;
                    next.viewIndex = nextViewIndex;
                    next.isLastPage = false;
                    next.isDoublePage = false;
                    next.viewCount = 1;
                }
            }
            else
            {
                next.path = null;
                next.zipPath = null;
                next.viewIndex = 0;
                next.isLastPage = false;
                next.isDoublePage = false;
                next.viewCount = 1;
            }
            lock.notify();
        }
    }

    private int getOptionViewMode()
    {
        return Option.getInstance().getOptionViewMode();
    }

    private int optionResizeMode()
    {
        return Option.getInstance().getDisplayOption();
    }

    private int optionResizeMethod()
    {
        return Option.getInstance().getResizeMethodOption();
    }

    private String optionFilter()
    {
        return Option.getInstance().getFilterOption();
    }

    private int drawImageFromPathToBitmap(String path, Bitmap bitmap, boolean isLastPage, int viewIndex)
    {
        return FreeImageWrapper.loadImageFromPath(bitmap, path, isLastPage, viewIndex, getOptionViewMode(), optionResizeMode(), optionResizeMethod(), optionFilter());
    }

    private int drawImageFromZipPathToBitmap(String zipPath, String path, Bitmap bitmap, boolean isLastPage, int viewIndex)
    {
        // inner path not start with /
        String innerPath = path;
        if(innerPath.charAt(0)=='/')
            innerPath = innerPath.substring(1);
        return FreeImageWrapper.loadImageFromZip(bitmap, zipPath, innerPath, isLastPage, viewIndex, getOptionViewMode(), optionResizeMode(), optionResizeMethod(), optionFilter());
    }

    private Bitmap getOutBitmap()
    {
        return isBitmap1Out==true ? mBitmap1 : mBitmap2;
    }

    private Bitmap getSubBitmap()
    {
        return isBitmap1Out==true ? mBitmap2 : mBitmap1;
    }

    @Override protected void onDraw(Canvas canvas)
    {
        if(Thread.holdsLock(lock)==true)
        {
            postInvalidateDelayed(100);
            Log.d("FASTIMAGE", "onDraw Delayed");
            return;
        }

        Log.d("FASTIMAGE", "onDraw Start");
        synchronized(lock)
        {
            if(current.complete==true)
            {
                currentDraw = true;
                canvas.drawBitmap(getOutBitmap(), 0, 0, null);
                Log.d("FASTIMAGE", "onDraw OK");
            }
            else
            {
                postInvalidateDelayed(100);
                Log.d("FASTIMAGE", "onDraw Skip");
            }
            lock.notify();
        }
    }
}

class FastImageViewLocation
{
    public boolean complete = false;
    public String path;
    public String zipPath;
    public int viewIndex;
    public boolean isLastPage;
    volatile public boolean isDoublePage;
    volatile public int viewCount; // volatile

    public FastImageViewLocation clone()
    {
        FastImageViewLocation obj = new FastImageViewLocation();
        obj.copyFrom(this);
        return obj;
    }

    public void copyFrom(FastImageViewLocation obj)
    {
        complete = obj.complete;
        path = obj.path;
        zipPath = obj.zipPath;
        viewIndex = obj.viewIndex;
        isLastPage = obj.isLastPage;
        isDoublePage = obj.isDoublePage;
        viewCount = obj.viewCount;
    }

    public int getAllViewCount()
    {
        return isDoublePage ? viewCount*2 : viewCount;
    }

    public boolean hasNextView()
    {
        return viewIndex+1<getAllViewCount();
    }

    public boolean hasPrevView()
    {
        return viewIndex>=1;
    }

    public int getNextViewIndex()
    {
        if(hasNextView())
            return viewIndex+1;
        return -1;
    }

    public int getPrevViewIndex(boolean top)
    {
        if(hasPrevView())
        {
            if(top && viewIndex==viewCount)
                return 0;
            return viewIndex-1;
        }
        return -1;
    }

    public boolean isReady(String targetPath, int targetViewIndex)
    {
        return complete==true && path!=null && path.compareTo(targetPath)==0 && viewIndex==targetViewIndex;
    }

    public boolean isSameFile(String targetPath)
    {
        return path!=null && path.compareTo(targetPath)==0;
    }
}

class FastImageViewPageController implements FastViewPageController
{
    private FastImageView fastView;
    private String path;
    private String zipPath;
    private String[] files;

    public FastImageViewPageController(FastImageView _fastView, String _path, String _zipPath, String[] _files)
    {
        fastView = _fastView;
        path = _path;
        zipPath = _zipPath;
        files = _files;
    }

    public int getStartPageIndex()
    {
        int index = 0;
        if(path!=null)
            index = getFileIndex(path);
        if(index<0)
            index = 0;
        return index;
    }

    public String getTitle()
    {
        if(zipPath!=null)
            return FileManager.getNameFromFullpath(zipPath);
        return FileManager.getNameFromFullpath( FileManager.getPathFromFullpath(path,"/root") );
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
        fastView.drawImageFromZipPath(zipPath, path, isPrev, viewIndex, prepareFilePath);

        saveLastView(zipPath, path, viewIndex);
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
