package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Context;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.deadwi.library.FreeImageWrapper;
import net.deadwi.library.MinizipWrapper;

import java.io.File;

public class FastImageActivity extends AppCompatActivity
{
    private FastImage fastView;
    private int width;
    private int height;
    private String zipPath;
    private String[] files;
    private int currntFileIndex;
    private Dialog pageControl;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        createPageControl();

        FreeImageWrapper.init();
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        width = dm.widthPixels;
        height = dm.heightPixels;
        Log.d("FASTIMAGE","Display : "+width+"x"+height);

        String path = getIntent().getStringExtra(FullscreenActivity.MSG_DATA_PATH);
        zipPath = getIntent().getStringExtra(FullscreenActivity.MSG_DATA_ZIP_PATH);
        files = getIntent().getStringArrayExtra(FullscreenActivity.MSG_DATA_FILES);
        if(path!=null)
            currntFileIndex = getFileIndex(path);
        if(currntFileIndex<0)
            currntFileIndex = 0;

        // page control
        if(zipPath!=null)
            setPageControlTitle( FileManager.getNameFromFullpath(zipPath) );
        else
            setPageControlTitle( FileManager.getNameFromFullpath( FileManager.getPathFromFullpath(path,"/root") ) );

        fastView = new FastImage(this, width, height);
        fastView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d("FASTIMAGE", "Touch " + event.getX() + "," + event.getY());

                    // Left side
                    if (event.getX() < (width / 4))
                    {
                        if(Option.getInstance().isRightTouchNextPage())
                            previousViewPage();
                        else
                            nextViewPage();
                    }
                    // Right side
                    else if (event.getX() > (width / 4 * 3))
                    {
                        if(Option.getInstance().isRightTouchNextPage())
                            nextViewPage();
                        else
                            previousViewPage();
                    }
                    else if (event.getY() < (height / 4))
                        closeViewPage();
                    else if (event.getY() > (height / 4 * 3))
                    {
                        setPageControlPage(currntFileIndex, files.length);
                        pageControl.show();
                    }
                    /*
                    else if (event.getY() > (height / 4 * 2) && event.getY() < (height / 4 * 3)) {
                        refreshEink();
                    }
                    */
                }
                return true;
            }
        });

        setContentView(fastView);
        fastView.startBackgroundLoader();
        requestImage(currntFileIndex, getIntent().getIntExtra("viewindex",0), false);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        fastView.stopBackgroundLoader();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(Option.getInstance().IsChanged())
        {
            fastView.clearImage();
            requestImage(currntFileIndex, fastView.getCurrent().viewIndex, false);
            Option.getInstance().setChanged(false);
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event)
    {
        Log.d("FASTIMAGE","key="+keycode);
        // ridibooks.com/Paper hardware key
        switch(keycode)
        {
            // prev
            case KeyEvent.KEYCODE_PAGE_UP:
                previousViewPage();
                break;
            // next
            case KeyEvent.KEYCODE_PAGE_DOWN:
                nextViewPage();
                break;
            // back
            case KeyEvent.KEYCODE_BACK:
                closeViewPage();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(Option.getInstance().getVolumeButtonOption()==Option.VOLUME_BUTTON_PREV_NEXT)
                    previousViewPage();
                else if(Option.getInstance().getVolumeButtonOption()==Option.VOLUME_BUTTON_NEXT_PREV)
                    nextViewPage();
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(Option.getInstance().getVolumeButtonOption()==Option.VOLUME_BUTTON_PREV_NEXT)
                    nextViewPage();
                else if(Option.getInstance().getVolumeButtonOption()==Option.VOLUME_BUTTON_NEXT_PREV)
                    previousViewPage();
                break;
        }
        return true;
    }

    private void createPageControl()
    {
        pageControl = new Dialog(this);
        pageControl.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pageControl.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pageControl.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        pageControl.getWindow().setGravity(Gravity.BOTTOM);
        pageControl.setContentView(R.layout.activity_page_control);
        pageControl.findViewById(R.id.buttonOption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(FastImageActivity.this, OptionTabActivity.class);
                startActivity(myIntent);
                overridePendingTransition(0, 0);
            }
        });
        pageControl.findViewById(R.id.buttonList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageControl.dismiss();
                closeViewPage();
            }
        });
        ((SeekBar)pageControl.findViewById(R.id.seekPageBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                currntFileIndex = progress;
                setPageControlPage(progress, files.length);
                requestImage(currntFileIndex, 0, false);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });


        //pageControl.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL , WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        WindowManager.LayoutParams params = pageControl.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        pageControl.getWindow().setAttributes(params);
    }

    private void setPageControlTitle(String title)
    {
        ((TextView)pageControl.findViewById(R.id.textTitle)).setText(title);
    }

    private void setPageControlPage(int page,int pageCount)
    {
        ((SeekBar)pageControl.findViewById(R.id.seekPageBar)).setMax(pageCount-1);
        ((SeekBar)pageControl.findViewById(R.id.seekPageBar)).setProgress(page);
        ((TextView)pageControl.findViewById(R.id.textPage)).setText((page+1) + "/" + pageCount);
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

    private void nextPage()
    {
        Log.d("FASTIMAGE","Page(next) : "+(currntFileIndex+1)+"/"+files.length);
        if (currntFileIndex>=0 && currntFileIndex < files.length - 1)
        {
            currntFileIndex++;
            requestImage(currntFileIndex, 0, false);
        }
        else
        {
            Toast.makeText(this.getApplicationContext(), R.string.MESSAGE_LAST_PAGE, Toast.LENGTH_SHORT).show();
        }
    }

    private void previousPage()
    {
        Log.d("FASTIMAGE","Page(prev) : "+(currntFileIndex+1)+"/"+files.length);
        if (currntFileIndex> 0)
        {
            currntFileIndex--;
            requestImage(currntFileIndex, 0, true);
        }
        else
        {
            Toast.makeText(this.getApplicationContext(), R.string.MESSAGE_FIRST_PAGE, Toast.LENGTH_SHORT).show();
        }
    }

    private void nextViewPage()
    {
        if(fastView.getCurrent().hasNextView())
        {
            requestImage(currntFileIndex, fastView.getCurrent().getNextViewIndex(), false);
        }
        else
            nextPage();
    }

    private void previousViewPage()
    {
        if(fastView.getCurrent().hasPrevView())
        {
            requestImage(currntFileIndex, fastView.getCurrent().getPrevViewIndex(true), false);
        }
        else
            previousPage();
    }

    private void closeViewPage()
    {
        if(zipPath!=null)
        {
            String dir = FileManager.getPathFromFullpath(zipPath, "/");
            BookmarkItem item = new BookmarkItem();
            item.filename = FileManager.getNameFromFullpath(zipPath);
            item.innerName = (currntFileIndex>=0 && currntFileIndex <= files.length - 1) ? files[currntFileIndex] : "";
            item.fileIndex = currntFileIndex<0 ? 0 : currntFileIndex;
            item.fileCount = files.length;
            item.viewIndex = fastView.getCurrent().viewIndex;
            Bookmark.getInstance().updateBookmark(dir, item);
        }
        Option.getInstance().setLastView(null,null,0);
        Option.getInstance().saveLastView();

        finish();
        overridePendingTransition(0, 0);
    }

    private void requestImage(int fileIndex, int viewIndex, boolean isPrev)
    {
        if(fileIndex<0 || fileIndex>=files.length)
            return;
        String path = files[fileIndex];
        String prepareFilePath = fileIndex + 1 < files.length ? files[fileIndex + 1] : null;

        Log.d("FASTIMAGE","Request Page : file("+(fileIndex+1)+"/"+files.length+") view("
                +(viewIndex + 1) + "/" + fastView.getCurrent().getAllViewCount()
                +(isPrev ? " LAST" : "")+")");
        fastView.drawImageFromZipPath(zipPath, path, isPrev, viewIndex, prepareFilePath);

        Option.getInstance().setLastView(zipPath, path, viewIndex);
        Option.getInstance().saveLastView();
    }

    private void refreshEink()
    {
        Log.d("FASTIMAGE", "Refresh");
        final Instrumentation ins = new Instrumentation();

        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                int x = width / 2;
                int y = height - 20;
                long downTime = SystemClock.uptimeMillis();
                ins.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0));
                ins.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_UP, x, y, 0));

                downTime = SystemClock.uptimeMillis();
                ins.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0));
                ins.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_UP, x, y, 0));
            }
        });
        task.start();
    }
}

class ViewLocation
{
    public boolean complete = false;
    public String path;
    public String zipPath;
    public int viewIndex;
    public boolean isLastPage;
    volatile public boolean isDoublePage;
    volatile public int viewCount; // volatile

    public ViewLocation clone()
    {
        ViewLocation obj = new ViewLocation();
        obj.copyFrom(this);
        return obj;
    }

    public void copyFrom(ViewLocation obj)
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

@SuppressLint("ViewConstructor")
class FastImage extends View implements Runnable
{
    private boolean isBitmap1Out = true;
    private Object lock = new Object();
    private Bitmap mBitmap1;
    private Bitmap mBitmap2;

    private boolean isThreadRun = false;
    private Thread loaderThread;

    private boolean currentDraw = false;
    private ViewLocation current;
    private ViewLocation next;

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
                            current.isDoublePage = ret>=FreeImageWrapper.RETURN_PAGE_UNIT;
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

    public FastImage(Context context, int width, int height)
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

        current = new ViewLocation();
        next = new ViewLocation();
    }

    public ViewLocation getCurrent()
    {
        return current;
    }

    public void clearImage()
    {
        synchronized(lock)
        {
            current.complete = false;
            next.complete = false;
        }
    }

    public void drawImageFromZipPath(String zipPath, String path, boolean isLastPage, int viewIndex, String nextPath)
    {
        synchronized(lock)
        {
            if(current.complete==false && next.complete==false)
            {
                // wait;

            }

            ViewLocation tcurrent = current.clone();
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
