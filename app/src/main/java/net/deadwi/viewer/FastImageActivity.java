package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Context;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.Display;
import android.view.WindowManager;

import net.deadwi.library.FreeImageWrapper;
import net.deadwi.library.MinizipWrapper;

public class FastImageActivity extends AppCompatActivity
{
    private FastImage fastView;
    private int width;
    private int height;
    private String zipPath;
    private String[] files;
    private int fileIndex;
    private int viewPage;
    private int maxViewPage;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FreeImageWrapper.init();
        width = getWindowManager().getDefaultDisplay().getWidth();
        height = getWindowManager().getDefaultDisplay().getHeight();

        String path = getIntent().getStringExtra("path");
        zipPath = getIntent().getStringExtra("zipPath");
        files = getIntent().getStringArrayExtra("files");

        fileIndex = -1;
        for(int i=0;i<files.length;i++)
        {
            if(files[i]==null)
                continue;;

            if(path.compareTo(files[i])==0)
            {
                fileIndex = i;
                break;
            }
        }
        viewPage = 0;
        maxViewPage = 1;

        fastView = new FastImage(this, width, height);
        fastView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    Log.d("FASTIMAGE", "Touch " + event.getX() + "," + event.getY());

                    if (event.getX() < (width / 4))
                        previousViewPage();
                    else if (event.getX() > (width / 4 * 3))
                        nextViewPage();
                    else if (event.getY() < (height / 4)) {
                        finish();
                        overridePendingTransition(0, 0);
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
        drawImage();
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

    private void nextPage()
    {
        if (fileIndex > 0)
        {
            fileIndex--;
            viewPage = 0;
            maxViewPage = 1;
            drawImage();
        }
    }

    private void previousPage()
    {
        if (fileIndex>=0 && fileIndex < files.length - 1)
        {
            fileIndex++;
            viewPage = 0;
            maxViewPage = 1;
            drawImage();
        }
    }

    private void nextViewPage()
    {
        maxViewPage = fastView.getCurrentMaxViewPage();
        Log.d("FASTIMAGE","View page(next) : "+(viewPage+1)+" / "+maxViewPage);

        if(viewPage+1<maxViewPage)
        {
            viewPage++;
            drawImage();
        }
        else
            nextPage();
    }

    private void previousViewPage()
    {
        Log.d("FASTIMAGE","View page(prev) : "+(viewPage+1)+" / "+maxViewPage);
        if(viewPage>=1 && maxViewPage>=2)
        {
            viewPage--;
            drawImage();
        }
        else
            previousPage();
    }

    private void drawImage()
    {
        if(fileIndex<0 || fileIndex>=files.length)
            return;
        String path = files[fileIndex];

        if(zipPath!=null)
            fastView.drawImageFromZipPath(zipPath, path, (fileIndex+1<files.length ? files[fileIndex+1] : null), viewPage);
        else
            fastView.drawImageFromPath(path, viewPage);
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

@SuppressLint("ViewConstructor")
class FastImage extends View implements Runnable
{
    static private boolean isBitmap1Out = true;
    static private Object lock = new Object();
    static private Bitmap mBitmap1;
    static private Bitmap mBitmap2;

    private boolean isThreadRun = false;
    private Thread loaderThread;

    private boolean currentDraw = false;

    private boolean currentComplete = false;
    private String currentPath;
    private String currentZipPath;
    private int currentViewPage;
    private volatile int currentMaxViewPage;

    private boolean prepareComplete = false;
    private String preparePath;
    private String prepareZipPath;
    private int prepareViewPage;

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
        while(isThreadRun)
        {
            synchronized(lock)
            {
                Log.d("FASTIMAGE2", "Loop");
                lock.notify();
                try
                {
                    if(currentComplete==false || currentDraw==false || prepareComplete==true)
                        lock.wait();
                }
                catch (InterruptedException e)
                {
                }

                if(currentComplete==false)
                {
                    if(currentPath!=null)
                    {
                        Log.d("FASTIMAGE2", "Current loading");
                        if (currentZipPath == null)
                            currentMaxViewPage = drawImageFromPathToBitmap(currentPath, getOutBitmap(), currentViewPage);
                        else
                            currentMaxViewPage = drawImageFromZipPathToBitmap(currentZipPath, currentPath, getOutBitmap(), currentViewPage);
                        Log.d("FASTIMAGE2", "Current loaded : view count="+currentMaxViewPage);
                    }
                    currentComplete = true;
                    postInvalidate();
                }
                if(prepareComplete==false && currentDraw==true)
                {
                    if(preparePath!=null)
                    {
                        Log.d("FASTIMAGE2", "Prepare loading");
                        if(prepareZipPath==null)
                            drawImageFromPathToBitmap(preparePath, getSubBitmap(), prepareViewPage);
                        else
                            drawImageFromZipPathToBitmap(prepareZipPath, preparePath, getSubBitmap(), prepareViewPage);
                        Log.d("FASTIMAGE2", "Prepare loaded");
                    }
                    prepareComplete = true;
                }
            }
        }
    }

    public FastImage(Context context, int width, int height)
    {
        super(context);
        if(mBitmap1==null)
        {
            mBitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            mBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }
    }

    public int getCurrentMaxViewPage()
    {
        // lock?
        return currentMaxViewPage;
    }

    public void drawImageFromPath(String path, int viewPage)
    {
        synchronized(lock)
        {
            currentComplete = false;
            currentPath = path;
            currentZipPath = null;
            currentViewPage = viewPage;
            currentMaxViewPage = viewPage+1;
            lock.notify();
        }
    }

    public void drawImageFromZipPath(String zipPath, String path, String nextPath, int viewPage)
    {
        synchronized(lock)
        {
            boolean tcurrentComplete = currentComplete;
            String tcurrentPath = currentPath;
            String tcurrentZipPath = currentZipPath;

            currentDraw = false;
            // next page is ready
            if(prepareComplete==true && preparePath!=null && preparePath.compareTo(path)==0)
            {
                currentComplete = true;
                currentPath = path;
                currentZipPath = zipPath;
                currentViewPage = viewPage;
                currentMaxViewPage = viewPage+1;

                isBitmap1Out = !isBitmap1Out;
                postInvalidate();
            }
            else
            {
                currentComplete = false;
                currentPath = path;
                currentZipPath = zipPath;
                currentViewPage = viewPage;
                currentMaxViewPage = viewPage+1;
            }

            prepareComplete = false;
            if(nextPath!=null)
            {
                // next page is ready
                if(tcurrentComplete==true && nextPath!=null && nextPath.compareTo(tcurrentPath)==0)
                {
                    prepareComplete = true;
                    isBitmap1Out = !isBitmap1Out;
                }
                preparePath = nextPath;
                prepareZipPath = zipPath;
            }
            else
            {
                preparePath = null;
                prepareZipPath = null;
            }
            lock.notify();
        }
    }

    private int drawImageFromPathToBitmap(String path, Bitmap bitmap, int viewPage)
    {
        return FreeImageWrapper.loadImageFromPath(bitmap, path, viewPage);
    }

    private int drawImageFromZipPathToBitmap(String zipPath, String path, Bitmap bitmap, int viewPage)
    {
        // inner path not start with /
        String innerPath = path;
        if(innerPath.charAt(0)=='/')
            innerPath = innerPath.substring(1);
        return FreeImageWrapper.loadImageFromZip(bitmap, zipPath, innerPath, viewPage);
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
            if(currentComplete==true)
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
