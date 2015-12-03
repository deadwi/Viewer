package net.deadwi.viewer;

import net.deadwi.library.FreeImageWrapper;
import net.deadwi.library.MinizipWrapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by jihun.jo on 2015-11-18.
 */
public class Option {
    public static final int SPLIT_AUTO=0;
    public static final int SPLIT_SINGLE=1;
    public static final int SPLIT_DOUBLE=2;
    public static final int DISPLAY_FIT= FreeImageWrapper.DISPLAY_FIT;
    public static final int DISPLAY_WIDTH= FreeImageWrapper.DISPLAY_WIDTH;
    public static final int DISPLAY_HEIGHT= FreeImageWrapper.DISPLAY_HEIGHT;
    public static final int RESIZE_METHOD_BOX= FreeImageWrapper.RESIZE_METHOD_BOX;
    public static final int RESIZE_METHOD_BILINEAR= FreeImageWrapper.RESIZE_METHOD_BILINEAR;
    public static final int RESIZE_METHOD_BSPLINE= FreeImageWrapper.RESIZE_METHOD_BSPLINE;
    public static final int RESIZE_METHOD_CATMULLROM= FreeImageWrapper.RESIZE_METHOD_CATMULLROM;
    public static final int RESIZE_METHOD_LANCZOS3= FreeImageWrapper.RESIZE_METHOD_LANCZOS3;

    private static Option ourInstance = new Option();
    private Properties optionProperties;
    private Properties lastProperties;
    private String propertiesPath;

    private boolean isChanged;

    private boolean isPortrait;
    private boolean isReadLeftToRight;
    private int splitOption;
    private int displayOption;
    private int resizeMethodOption;
    private boolean isEnableFilterGray;
    private int filterGrayThreshold;

    private String lastCurrentPath;
    private String lastInnerPath;
    private String lastViewPath;
    private String lastViewZipPath;
    private int lastViewIndex;

    public static Option getInstance() {
        return ourInstance;
    }

    private Option() {
        optionProperties = new Properties();
        lastProperties = new Properties();

        isChanged = false;
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

    public void loadOption(String path)
    {
        propertiesPath = path;
        try
        {
            optionProperties.load(new FileInputStream(propertiesPath+"/option.properties"));
        }
        catch (IOException e)
        {
        }
        try
        {
            lastProperties.load(new FileInputStream(propertiesPath + "/last.properties"));
        }
        catch (IOException e)
        {
        }

        isPortrait = optionProperties.getProperty("option_portrait","true").compareTo("true")==0;
        isReadLeftToRight = optionProperties.getProperty("option_read_left_to_right","true").compareTo("true")==0;
        splitOption = Integer.parseInt(optionProperties.getProperty("option_split",""+SPLIT_AUTO));
        displayOption = Integer.parseInt(optionProperties.getProperty("option_display",""+DISPLAY_FIT));
        resizeMethodOption = Integer.parseInt(optionProperties.getProperty("option_resize_method",""+RESIZE_METHOD_BILINEAR));

        lastCurrentPath = lastProperties.getProperty("last_current_path", null);
        lastInnerPath = lastProperties.getProperty("last_inner_path", null);
        lastViewPath = lastProperties.getProperty("last_view_path", null);
        lastViewZipPath = lastProperties.getProperty("last_view_zip_path",null);
        lastViewIndex = Integer.parseInt(lastProperties.getProperty("last_view_index","0"));
    }

    public void saveOption()
    {
        optionProperties.setProperty("option_portrait",(isPortrait ? "true" : "false"));
        optionProperties.setProperty("option_read_left_to_right",(isReadLeftToRight ? "true" : "false"));
        optionProperties.setProperty("option_split",""+splitOption);
        optionProperties.setProperty("option_display",""+displayOption);
        optionProperties.setProperty("option_resize_method", "" + resizeMethodOption);

        try
        {
            optionProperties.store(new FileOutputStream(propertiesPath + "/option.properties"), null);
        }
        catch(IOException e)
        {
        }
    }

    public void saveLastPath()
    {
        if(lastCurrentPath!=null)
            lastProperties.setProperty("last_current_path", lastCurrentPath);
        else
            lastProperties.remove("last_current_path");

        if(lastInnerPath!=null)
            lastProperties.setProperty("last_inner_path", lastInnerPath);
        else
            lastProperties.remove("last_inner_path");
        saveLastProperty();
    }

    public void saveLastView()
    {
        if(lastViewPath!=null)
            lastProperties.setProperty("last_view_path",lastViewPath);
        else
            lastProperties.remove("last_view_path");
        if(lastViewZipPath!=null)
            lastProperties.setProperty("last_view_zip_path", lastViewZipPath);
        else
            lastProperties.remove("last_view_zip_path");
        lastProperties.setProperty("last_view_index", ""+lastViewIndex);
        saveLastProperty();
    }

    private void saveLastProperty()
    {
        try
        {
            lastProperties.store(new FileOutputStream(propertiesPath + "/last.properties"), null);
        }
        catch(IOException e)
        {
        }
    }

    public int getOptionViewMode()
    {
        switch (splitOption)
        {
            case Option.SPLIT_SINGLE:
                return FreeImageWrapper.VIEW_MODE_SINGLE;
            case Option.SPLIT_AUTO:
                return isReadLeftToRight ? FreeImageWrapper.VIEW_MODE_AUTO_L : FreeImageWrapper.VIEW_MODE_AUTO_R;
            case Option.SPLIT_DOUBLE:
                return isReadLeftToRight ? FreeImageWrapper.VIEW_MODE_DOUBLE_L : FreeImageWrapper.VIEW_MODE_DOUBLE_R;
        }
        return FreeImageWrapper.VIEW_MODE_SINGLE;
    }

    public String getFilterOption()
    {
        String optionStr = "";
        if(isEnableFilterGray)
        {
            optionStr += FreeImageWrapper.FILTER_GRAY_2BIT + "|" + filterGrayThreshold;
        }
        return optionStr;
    }

    public boolean IsChanged()
    {
        return isChanged;
    }
    public void setChanged(boolean _isChanged)
    {
        isChanged = _isChanged;
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
    public boolean IsEnableFilterGray() { return isEnableFilterGray; }
    public void setEnableFilterGray(boolean enable) { isEnableFilterGray = enable; }
    public int getFilterGrayThreshold() { return filterGrayThreshold; }
    public void setFilterGrayThreshold(int threshold) { filterGrayThreshold = threshold; }

    public String getLastCurrentPath() { return lastCurrentPath; }
    public String getLastInnerPath() { return lastInnerPath; }
    public void setLastPath(String currentPath, String innerPath)
    {
        lastCurrentPath = currentPath;
        lastInnerPath = innerPath;
    }
    public String getLastViewPath() { return lastViewPath; }
    public String getLastViewZipPath() { return lastViewZipPath; }
    public int getLastViewIndex() { return lastViewIndex; }
    public void setLastView(String zipPath, String path, int viewIndex)
    {
        lastViewPath = path;
        lastViewZipPath = zipPath;
        lastViewIndex = viewIndex;
    }
}
