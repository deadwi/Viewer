package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import net.deadwi.library.FreeImageWrapper;

import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.pdfdroid.codec.PdfContext;

import java.io.File;

/**
 * Created by jihun.jo on 2016-01-15.
 */
@SuppressLint("ViewConstructor")
public class PdfView extends DoubleBufferView
{
    public static final int RET_UNKNOWN_ERROR = -1;
    public static final int RET_DECRYPT_PDF = -2;
    private DecodeService decodeService;

    public PdfView(Context context, int width, int height)
    {
        super(context, width, height);
    }

    public int openPdfFile(String path)
    {
        int ret = RET_UNKNOWN_ERROR;
        try
        {
            decodeService = new DecodeServiceBase(new PdfContext());
            decodeService.setContentResolver(getContext().getContentResolver());
            decodeService.open(Uri.fromFile(new File(path)));
            return decodeService.getPageCount();
        }
        catch (RuntimeException ex)
        {
            if(ex.getMessage().indexOf("decrypt")>=0)
                ret = RET_DECRYPT_PDF;
        }
        return ret;
    }

    protected int drawImageFromPathToBitmap(String path, Bitmap bitmap, boolean isLastPage, int viewIndex)
    {
        CodecPage page = decodeService.getPage( Integer.parseInt(path) );
        int viewWidth = bitmap.getWidth();
        int viewHeight = bitmap.getHeight();
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();
        int[] areaSet = FreeImageWrapper.getOutputImageArea(isLastPage, viewIndex, getOptionViewMode(), optionResizeMode(), pageWidth, pageHeight, viewWidth, viewHeight);
        RectF pageRelativeBounds = new RectF(
                (float)areaSet[FreeImageWrapper.AREA_INDEX_ORIGIN_X]/(float)pageWidth,
                (float)areaSet[FreeImageWrapper.AREA_INDEX_ORIGIN_Y]/(float)pageHeight,
                (float)areaSet[FreeImageWrapper.AREA_INDEX_ORIGIN_X2]/(float)pageWidth,
                (float)areaSet[FreeImageWrapper.AREA_INDEX_ORIGIN_Y2]/(float)pageHeight
        );

        Bitmap outBitmap = page.renderBitmap(
                areaSet[FreeImageWrapper.AREA_INDEX_RESIZE_WIDTH],
                areaSet[FreeImageWrapper.AREA_INDEX_RESIZE_HEIGHT],
                pageRelativeBounds);

        bitmap.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(outBitmap,
                areaSet[FreeImageWrapper.AREA_INDEX_DISPLAY_X],
                areaSet[FreeImageWrapper.AREA_INDEX_DISPLAY_Y],
                null);
        outBitmap.recycle();
        return areaSet[FreeImageWrapper.AREA_INDEX_VIEW_COUNT]
                + FreeImageWrapper.RETURN_PAGE_UNIT*areaSet[FreeImageWrapper.AREA_INDEX_DOUBLE_PAGE];
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

        String path = ""+fileIndex;
        String prepareFilePath = (fileIndex + 1 < pageCount) ? ""+(fileIndex+1) : null;

        Log.d("PAGE", "Request Page : file(" + (fileIndex + 1) + "/" + pageCount + ") view("
                + (viewIndex + 1) + "/" + fastView.getAllViewCount()
                + (isPrev ? " LAST" : "") +")");
        fastView.drawImage(path, isPrev, viewIndex, prepareFilePath);
        //saveLastView(zipPath, path, viewIndex);
    }
}
