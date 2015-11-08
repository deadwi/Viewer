package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.Display;
import android.view.WindowManager;

import net.deadwi.library.MinizipWrapper;

public class FastImageActivity extends Activity
{
    private FastImage fastView;
    private int width;
    private int height;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        width = getWindowManager().getDefaultDisplay().getWidth();
        height = getWindowManager().getDefaultDisplay().getHeight();

        String path = getIntent().getStringExtra("path");
        String zipPath = getIntent().getStringExtra("zipPath");


        if(zipPath!=null)
        {
            Log.d("FASTIMAGE", "zip path : " + zipPath);
            fastView = new FastImage(this, width, height, zipPath, path);
        }
        else
            fastView = new FastImage(this, width, height, path);
        fastView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d("FASTIMAGE", "Touch " + event.getX() + "," + event.getY());

                    if (event.getY() < (height/4))
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

    static {
        System.loadLibrary("plasma");
    }
}

@SuppressLint("ViewConstructor")
class FastImage extends View
{
    private Bitmap mBitmap;
    Byte[] dataByte = new Byte[1024*512];

    // implementend by libplasma.so
    private static native void renderPlasma(Bitmap  bitmap, long time_ms);
    private static native void renderPlasma2(Bitmap  bitmap, String path);
    private static native void renderPlasma3(Bitmap  bitmap, Byte[] data, int dataSize);
    private static native void loadImage(String path);

    public FastImage(Context context, int width, int height, String path)
    {
        super(context);
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        renderPlasma2(mBitmap, path);
    }

    public FastImage(Context context, int width, int height, String zipPath, String path)
    {
        super(context);
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        // inner path not start with /
        String innerPath = path;
        if(innerPath.charAt(0)=='/')
            innerPath = innerPath.substring(1);

        MinizipWrapper minizip = new MinizipWrapper();
        int size = minizip.getFileData(zipPath,innerPath,null);
        Log.d("FASTIMAGE","unzip size : "+size);
        if(dataByte.length<size)
            dataByte = new Byte[size];

        int status = minizip.getFileData(zipPath,innerPath,dataByte);
        Log.d("FASTIMAGE","Get File Status : "+status);

        renderPlasma3(mBitmap, dataByte, size);
    }

    @Override protected void onDraw(Canvas canvas)
    {
        Log.d("FASTIMAGE", "DRAW");
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
}
