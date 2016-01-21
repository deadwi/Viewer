package net.deadwi.viewer;

import android.content.Context;
import android.view.View;

/**
 * Created by jihun.jo on 2016-01-15.
 */
public abstract class FastView extends View
{
    protected FastViewPageController pc;

    public FastView(Context context)
    {
        super(context);
    }
    public void setPageController(FastViewPageController _pc)
    {
        pc = _pc;
    }
    abstract public void startBackgroundLoader();
    abstract public void stopBackgroundLoader();
    abstract public void clearImage();
    abstract public int getViewIndex();
    abstract public int getAllViewCount();
    abstract public boolean hasNextView();
    abstract public boolean hasPrevView();
    abstract public int getNextViewIndex();
    abstract public int getPrevViewIndex(boolean top);
    abstract public void invalidateEInk();
}
