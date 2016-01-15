package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.pdfdroid.codec.PdfContext;

import java.io.File;

/**
 * Created by jihun.jo on 2016-01-15.
 */
@SuppressLint("ViewConstructor")
public class PdfView extends FastView
{
    private boolean isBitmap1Out = true;
    private Object lock = new Object();
    private Bitmap mBitmap1;
    private Bitmap mBitmap2;
    private Bitmap mOutBitmap;

    private boolean isThreadRun = false;
    private Thread loaderThread;

    private boolean currentDraw = false;
    private PdfViewLocation current;
    private PdfViewLocation next;

    private DecodeService decodeService;


    public PdfView(Context context, int width, int height)
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
    }

    public void startBackgroundLoader()
    {

    }

    public void stopBackgroundLoader()
    {

    }

    public void clearImage()
    {

    }

    public int getViewIndex()
    {
        return 0;
    }

    public int getAllViewCount()
    {
        return 0;
    }

    public boolean hasNextView()
    {
        return false;
    }

    public boolean hasPrevView()
    {
        return false;
    }

    public int getNextViewIndex()
    {
        return 0;
    }

    public int getPrevViewIndex(boolean top)
    {
        return 0;
    }

    public int openPdfFile(String path)
    {
        decodeService = new DecodeServiceBase(new PdfContext());
        decodeService.setContentResolver(getContext().getContentResolver());
        decodeService.open(Uri.fromFile(new File(path)));
        return decodeService.getPageCount();
    }

    public int drawImage(int pageIndex)
    {
        CodecPage page = decodeService.getPage(pageIndex);
        int viewWidth = mBitmap1.getWidth();
        int viewHeight = mBitmap1.getHeight();
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();
        RectF pageRelativeBounds = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        if(mOutBitmap!=null)
            mOutBitmap.recycle();
        mOutBitmap = page.renderBitmap(viewWidth, viewHeight, pageRelativeBounds);
        postInvalidate();

        Log.d("PDF", "out size : " + mOutBitmap.getWidth() + " " + mOutBitmap.getHeight());
        Log.d("PDF", "drawImage complete");
        return 0;
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

    private Bitmap getOutBitmap()
    {
        return isBitmap1Out==true ? mBitmap1 : mBitmap2;
    }

    private Bitmap getSubBitmap()
    {
        return isBitmap1Out==true ? mBitmap2 : mBitmap1;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Log.d("PDF", "onDraw Start");
        if(mOutBitmap!=null)
        {
            Log.d("PDF", "onDraw draw");
            canvas.drawBitmap(mOutBitmap, 0, 0, null);
        }
        else {
            postInvalidateDelayed(100);
        }
    }
}

class PdfViewLocation
{
    public boolean complete = false;
    public int viewIndex;
    public boolean isLastPage;
    volatile public boolean isDoublePage;
    volatile public int viewCount;

    public PdfViewLocation clone()
    {
        PdfViewLocation obj = new PdfViewLocation();
        obj.copyFrom(this);
        return obj;
    }

    public void copyFrom(PdfViewLocation obj)
    {
        complete = obj.complete;
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
}

class PdfViewPageController implements FastViewPageController
{
    private PdfView fastView;
    private String path;
    int pageCount;

    public PdfViewPageController(PdfView _fastView, String _path)
    {
        fastView = _fastView;
        path = _path;
        pageCount = fastView.openPdfFile(path);
    }

    public int getStartPageIndex()
    {
        return 0;
    }

    public String getTitle()
    {
        return FileManager.getNameFromFullpath(path);
    }

    public int getPageCount()
    {
        return pageCount;
    }

    public void saveBookmark(int pageIndex, int viewIndex)
    {
    }

    public void requestPage(int fileIndex, int viewIndex, boolean isPrev)
    {
        if(fileIndex<0 || fileIndex>=pageCount)
            return;
        fastView.drawImage(fileIndex);
    }
}
