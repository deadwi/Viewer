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
    public static final int RESIZE_METHOD_LANCZOS3=4;

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
        loadOption();
    }

    public void resetDefaultOption()
    {
        isPortrait = true;
        isReadLeftToRight = true;
        splitOption = SPLIT_AUTO;
        displayOption = DISPLAY_FIT;
        resizeMethodOption = RESIZE_METHOD_BILINEAR;
    }

    public void loadOption()
    {
        isPortrait = System.getProperty("option_portrait","true").compareTo("true")==0;
        isReadLeftToRight = System.getProperty("option_read_left_to_right","true").compareTo("true")==0;
        splitOption = Integer.parseInt(System.getProperty("option_split",""+SPLIT_AUTO));
        displayOption = Integer.parseInt(System.getProperty("option_display",""+DISPLAY_FIT));
        resizeMethodOption = Integer.parseInt(System.getProperty("option_resize_method",""+RESIZE_METHOD_BILINEAR));
    }

    public void saveOption()
    {
        System.setProperty("option_portrait",(isPortrait ? "true" : "false"));
        System.setProperty("option_read_left_to_right",(isReadLeftToRight ? "true" : "false"));
        System.setProperty("option_split",""+splitOption);
        System.setProperty("option_display",""+displayOption);
        System.setProperty("option_resize_method",""+resizeMethodOption);
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
