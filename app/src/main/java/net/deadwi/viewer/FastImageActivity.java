package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.deadwi.library.FreeImageWrapper;

public class FastImageActivity extends AppCompatActivity
{
    private FastView fastView;
    private FastViewPageController pc;
    private int width;
    private int height;
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
        String type = getIntent().getStringExtra(FullscreenActivity.MSG_DATA_VIEW_TYPE);
        Log.d("FASTIMAGE","Display("+type+") : "+width+"x"+height);

        int viewIndex = getIntent().getIntExtra(FullscreenActivity.MSG_DATA_VIEW_INDEX,0);
        if(type.compareTo(FullscreenActivity.VIEW_TYPE_IMAGE)==0)
        {
            fastView = new FastImageView(this, width, height);
            pc = new FastImageViewPageController((FastImageView) fastView,
                    getIntent().getStringExtra(FullscreenActivity.MSG_DATA_PATH),
                    getIntent().getStringExtra(FullscreenActivity.MSG_DATA_ZIP_PATH),
                    getIntent().getStringArrayExtra(FullscreenActivity.MSG_DATA_FILES));
        }
        else if(type.compareTo(FullscreenActivity.VIEW_TYPE_PDF)==0)
        {
            fastView = new PdfView(this, width, height);
            pc = new PdfViewPageController((PdfView)fastView,
                    getIntent().getStringExtra(FullscreenActivity.MSG_DATA_PATH));
        }

        currntFileIndex = pc.getStartPageIndex();
        setPageControlTitle( pc.getTitle() );


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
                        setPageControlPage(currntFileIndex, pc.getPageCount());
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
        requestImage(currntFileIndex, viewIndex, false);
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
            requestImage(currntFileIndex, fastView.getViewIndex(), false);
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
            case KeyEvent.KEYCODE_MENU:
                setPageControlPage(currntFileIndex, pc.getPageCount());
                pageControl.show();
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
                setPageControlPage(progress, pc.getPageCount());
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

    private void nextPage()
    {
        Log.d("FASTIMAGE","Page(next) : "+(currntFileIndex+1)+"/"+pc.getPageCount());
        if (currntFileIndex>=0 && currntFileIndex < pc.getPageCount() - 1)
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
        Log.d("FASTIMAGE","Page(prev) : "+(currntFileIndex+1)+"/"+pc.getPageCount());
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
        if(fastView.hasNextView())
        {
            requestImage(currntFileIndex, fastView.getNextViewIndex(), false);
        }
        else
            nextPage();
    }

    private void previousViewPage()
    {
        if(fastView.hasPrevView())
        {
            requestImage(currntFileIndex, fastView.getPrevViewIndex(true), false);
        }
        else
            previousPage();
    }

    private void closeViewPage()
    {
        pc.saveBookmark(currntFileIndex, fastView.getViewIndex());
        finish();
        overridePendingTransition(0, 0);
    }

    private void requestImage(int fileIndex, int viewIndex, boolean isPrev)
    {
        pc.requestPage(fileIndex, viewIndex, isPrev);
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
