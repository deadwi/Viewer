package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.content.Context;
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

        if(zipPath!=null)
        {
            Log.d("FASTIMAGE", "zip path : " + zipPath);
            fastView = new FastImage(this, width, height, zipPath, path);
        }
        else
            fastView = new FastImage(this, width, height, path);
        fastView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    Log.d("FASTIMAGE", "Touch " + event.getX() + "," + event.getY());

                    if (event.getX() < (width/4))
                    {
                        if(fileIndex>0)
                        {
                            fileIndex--;
                            drawImage();
                        }
                    }
                    else if (event.getX() > (width/4*3))
                    {
                        if(fileIndex<files.length-1)
                        {
                            fileIndex++;
                            drawImage();
                        }
                    }
                    else if (event.getY() < (height/4))
                    {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                }
                return true;
            }
        });

        setContentView(fastView);
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

    private void drawImage()
    {
        if(fileIndex<0 || fileIndex>=files.length)
            return;
        String path = files[fileIndex];
        if(zipPath!=null)
            fastView.drawImageFromZipPath(zipPath, path, (fileIndex+1<files.length ? files[fileIndex+1] : null));
        else
            fastView.drawImageFromPath(path);
    }
}

@SuppressLint("ViewConstructor")
class FastImage extends View
{
    static private Bitmap mBitmap;
    static private Bitmap mBitmap2;
    private String prepareZipPath;
    private String prepareInnerPath;

    public FastImage(Context context, int width, int height, String path)
    {
        super(context);
        if(mBitmap==null)
        {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            mBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }
        drawImageFromPath(path);
    }

    public FastImage(Context context, int width, int height, String zipPath, String path)
    {
        super(context);
        if(mBitmap==null)
        {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            mBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }
        drawImageFromZipPath(zipPath, path, null);
    }

    public void drawImageFromPath(String path)
    {
        FreeImageWrapper.loadImageFromPath(mBitmap, path);
        invalidate();
    }

    public void drawImageFromZipPath(String zipPath, String path, String nextPath)
    {
        // inner path not start with /
        String innerPath = path;
        if(innerPath.charAt(0)=='/')
            innerPath = innerPath.substring(1);

        if(nextPath!=null)
        {
            prepareZipPath = zipPath;
            prepareInnerPath = nextPath;
        }

        FreeImageWrapper.loadImageFromZip(mBitmap, zipPath, innerPath);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas)
    {
        Log.d("FASTIMAGE", "DRAW");
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
}
