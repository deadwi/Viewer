package net.deadwi.viewer;

/**
 * Created by jihun.jo on 2015-11-18.
 */
public class Option {
    public static final int SPLIT_AUTO=0;
    public static final int SPLIT_SINGLE=1;
    public static final int SPLIT_DOUBLE=2;
    public static final int DISPLAY_FIT=0;
    public static final int DISPLAY_WIDTH=1;
    public static final int DISPLAY_HEIGHT=2;
    public static final int RESIZE_METHOD_BOX=0;
    public static final int RESIZE_METHOD_BILINEAR=1;
    public static final int RESIZE_METHOD_BSPLINE=2;
    public static final int RESIZE_METHOD_CATMULLROM=3;

    private static Option ourInstance = new Option();
    private boolean isPortrait;
    private boolean isReadLeftToRight;
    private int splitOption;
    private int displayOption;
    private int resizeMethodOption;

    public static Option getInstance() {
        return ourInstance;
    }

    private Option() {
        resetDefaultOption();
    }

    public void resetDefaultOption()
    {
        isPortrait = true;
        isReadLeftToRight = true;
        splitOption = SPLIT_AUTO;
        displayOption = DISPLAY_FIT;
        resizeMethodOption = RESIZE_METHOD_BILINEAR;
    }

    public boolean IsPortrait()
    {
        return isPortrait;
    }
    public void setPortrait(boolean _isPortrait)
    {
        isPortrait = _isPortrait;
    }
    public boolean IsReadLeftToRight()
    {
        return isReadLeftToRight;
    }
    public void setReadDirection(boolean _isLeftToRight)
    {
        isReadLeftToRight = _isLeftToRight;
    }
    public int getSplitOption()
    {
        return splitOption;
    }
    public void setSplitOption(int option)
    {
        splitOption = option;
    }
    public int getDisplayOption()
    {
        return displayOption;
    }
    public void setDisplayOption(int option)
    {
        displayOption = option;
    }
    public int getResizeMethodOption()
    {
        return resizeMethodOption;
    }
    public void setResizeMethodOption(int option)
    {
        resizeMethodOption = option;
    }
}
