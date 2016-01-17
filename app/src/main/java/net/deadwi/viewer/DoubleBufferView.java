package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import net.deadwi.library.FreeImageWrapper;

@SuppressLint("ViewConstructor")
abstract class DoubleBufferView extends FastView implements Runnable
{
    private boolean isBitmap1Out = true;
    private Object lock = new Object();
    private Bitmap mBitmap1;
    private Bitmap mBitmap2;

    private boolean isThreadRun = false;
    private Thread loaderThread;

    private boolean currentDraw = false;
    private DoubleBufferViewLocation current;
    private DoubleBufferViewLocation next;

    abstract protected int drawImageFromPathToBitmap(String path, Bitmap bitmap, boolean isLastPage, int viewIndex);

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
                Log.d("DBV", "Loop");
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
                        Log.d("DBV", "Current loading : "+current.path+" viewIndex : "+current.viewIndex + (current.isLastPage ? "(LAST)" : ""));
                        ret = drawImageFromPathToBitmap(current.path, getOutBitmap(), current.isLastPage, current.viewIndex);
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
                            Log.d("DBV", "Current loaded : all view count="+current.getAllViewCount());

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
                            Log.e("DBV", "Current loading fail(code="+ret+")");
                        }
                    }
                    current.complete = true;
                    postInvalidate();
                }
                else if(next.complete==false && currentDraw==true)
                {
                    if(next.path!=null)
                    {
                        Log.d("DBV", "Next loading : "+next.path+" viewIndex : "+next.viewIndex);
                        ret = drawImageFromPathToBitmap(next.path, getSubBitmap(), false, next.viewIndex);
                        if(ret>=0)
                        {
                            next.isDoublePage = ret >= FreeImageWrapper.RETURN_PAGE_UNIT;
                            next.viewCount = ret % FreeImageWrapper.RETURN_PAGE_UNIT;
                            Log.d("DBV", "Next loaded : all view count="+next.getAllViewCount());
                        }
                        else
                        {
                            Log.e("DBV", "Next loading fail(code="+ret+")");
                        }
                    }
                    next.complete = true;
                }
            }
        }
    }

    public DoubleBufferView(Context context, int width, int height)
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

        current = new DoubleBufferViewLocation();
        next = new DoubleBufferViewLocation();
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

    public void drawImage(String path, boolean isLastPage, int viewIndex, String nextPath)
    {
        synchronized(lock)
        {
            if(current.complete==false && next.complete==false)
            {
                // wait;
            }

            DoubleBufferViewLocation tcurrent = current.clone();
            int nextViewIndex=0;

            currentDraw = false;
            // next request
            // request page is ready
            if(next.isReady(path,viewIndex))
            {
                current.copyFrom(next);

                Log.d("DBV", "Current page prepared");
                isBitmap1Out = !isBitmap1Out;
                postInvalidate();
            }
            else
            {
                current.complete = false;
                current.path = path;
                current.viewIndex = viewIndex;
                current.isLastPage = isLastPage;
                current.isDoublePage = false;
                current.viewCount = viewIndex+1;

                if(tcurrent.isSamePath(path)==true)
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
                Log.d("DBV", "Next page view : "+nextViewIndex);

                // prev request
                // next of request's  page is ready
                if(tcurrent.isReady(nextPath,nextViewIndex))
                {
                    next.copyFrom(tcurrent);

                    Log.d("DBV", "Next page prepared");
                    isBitmap1Out = !isBitmap1Out;
                }
                else
                {
                    next.path = nextPath;
                    next.viewIndex = nextViewIndex;
                    next.isLastPage = false;
                    next.isDoublePage = false;
                    next.viewCount = 1;
                }
            }
            else
            {
                next.path = null;
                next.viewIndex = 0;
                next.isLastPage = false;
                next.isDoublePage = false;
                next.viewCount = 1;
            }
            lock.notify();
        }
    }

    protected int getOptionViewMode()
    {
        return Option.getInstance().getOptionViewMode();
    }

    protected int optionResizeMode()
    {
        return Option.getInstance().getDisplayOption();
    }

    protected int optionResizeMethod()
    {
        return Option.getInstance().getResizeMethodOption();
    }

    protected String optionFilter()
    {
        return Option.getInstance().getFilterOption();
    }

    protected Bitmap getOutBitmap()
    {
        return isBitmap1Out==true ? mBitmap1 : mBitmap2;
    }

    protected Bitmap getSubBitmap()
    {
        return isBitmap1Out==true ? mBitmap2 : mBitmap1;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(Thread.holdsLock(lock)==true)
        {
            postInvalidateDelayed(100);
            Log.d("DBV", "onDraw Delayed");
            return;
        }

        Log.d("DBV", "onDraw Start");
        synchronized(lock)
        {
            if(current.complete==true)
            {
                currentDraw = true;
                canvas.drawBitmap(getOutBitmap(), 0, 0, null);
                Log.d("DBV", "onDraw OK");
            }
            else
            {
                postInvalidateDelayed(100);
                Log.d("DBV", "onDraw Skip");
            }
            lock.notify();
        }
    }
}

class DoubleBufferViewLocation
{
    public boolean complete = false;
    public String path;
    public int viewIndex;
    public boolean isLastPage;
    volatile public boolean isDoublePage;
    volatile public int viewCount; // volatile

    public DoubleBufferViewLocation clone()
    {
        DoubleBufferViewLocation obj = new DoubleBufferViewLocation();
        obj.copyFrom(this);
        return obj;
    }

    public void copyFrom(DoubleBufferViewLocation obj)
    {
        complete = obj.complete;
        path = obj.path;
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

    public boolean isSamePath(String targetPath)
    {
        return path!=null && path.compareTo(targetPath)==0;
    }
}
