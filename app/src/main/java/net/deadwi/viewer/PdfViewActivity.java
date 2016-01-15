package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import net.deadwi.library.FreeImageWrapper;
import net.deadwi.pdf.PDFView;

import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.pdfdroid.codec.PdfContext;

import java.io.File;

public class PdfViewActivity extends AppCompatActivity
{
    private int currntFileIndex;
    private int width;
    private int height;
    PDFView pdfView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        width = dm.widthPixels;
        height = dm.heightPixels;

        String path = getIntent().getStringExtra(FullscreenActivity.MSG_DATA_PATH);
        if(currntFileIndex<0)
            currntFileIndex = 0;

        pdfView = new PDFView(this,null);
        pdfView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
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
                        //setPageControlPage(currntFileIndex, files.length);
                        //pageControl.show();
                    }
                }
                return true;
            }
        });

        pdfView.fromFile(new File(path))
                .defaultPage(0)
                .enableSwipe(false)
                .load();
        setContentView(pdfView);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
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
    }

    private void nextViewPage()
    {
        /*
        Log.d("PDF","next page "+pdfView.getCurrentPage()+"/"+pdfView.getPageCount());
        if(pdfView.getCurrentPage()<pdfView.getPageCount())
            pdfView.jumpTo(pdfView.getCurrentPage() + 2);
        */
    }

    private void previousViewPage()
    {
        /*
        Log.d("PDF","prev page "+pdfView.getCurrentPage()+"/"+pdfView.getPageCount());
        if(pdfView.getCurrentPage() > 1)
            pdfView.jumpTo( pdfView.getCurrentPage()-1 );
        */
    }

    private void closeViewPage()
    {
        finish();
        overridePendingTransition(0, 0);
    }
}

