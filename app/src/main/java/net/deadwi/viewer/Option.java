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
    public static final int TOUCH_AUTO=0;
    public static final int TOUCH_PREV_NEXT=1;
    public static final int TOUCH_NEXT_PREV=2;
    public static final int VOLUME_BUTTON_DISABLE=0;
    public static final int VOLUME_BUTTON_PREV_NEXT=1;
    public static final int VOLUME_BUTTON_NEXT_PREV=2;
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
    private int touchOption;
    private int volumeButtonOption;
    private int displayOption;
    private int einkCleanOption;
    private int resizeMethodOption;
    private boolean isEnableColorBrightness;
    private int colorBrightnessValue;
    private boolean isEnableColorContrast;
    private int colorContrastValue;
    private boolean isEnableColorGamma;
    private double colorGammaValue;
    private boolean isEnableColorInvert;
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
        touchOption = TOUCH_AUTO;
        volumeButtonOption = VOLUME_BUTTON_DISABLE;
        displayOption = DISPLAY_FIT;
        einkCleanOption = 0;
        resizeMethodOption = RESIZE_METHOD_BILINEAR;

        isEnableColorBrightness=false;
        colorBrightnessValue=0;
        isEnableColorContrast=false;
        colorContrastValue=0;
        isEnableColorGamma=false;
        colorGammaValue=1.0;
        isEnableColorInvert=false;
        isEnableFilterGray=false;
        filterGrayThreshold=255;
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
        touchOption = Integer.parseInt(optionProperties.getProperty("option_touch",""+TOUCH_AUTO));
        volumeButtonOption = Integer.parseInt(optionProperties.getProperty("option_volume_button",""+VOLUME_BUTTON_DISABLE));
        splitOption = Integer.parseInt(optionProperties.getProperty("option_split",""+SPLIT_AUTO));
        displayOption = Integer.parseInt(optionProperties.getProperty("option_display",""+DISPLAY_FIT));
        einkCleanOption = Integer.parseInt(optionProperties.getProperty("option_eink_clean","0"));
        resizeMethodOption = Integer.parseInt(optionProperties.getProperty("option_resize_method",""+RESIZE_METHOD_BILINEAR));

        isEnableColorBrightness = optionProperties.getProperty("option_color_is_brightness","false").compareTo("true")==0;
        colorBrightnessValue = Integer.parseInt(optionProperties.getProperty("option_color_brightness","0"));
        isEnableColorContrast = optionProperties.getProperty("option_color_is_contrast","false").compareTo("true")==0;
        colorContrastValue = Integer.parseInt(optionProperties.getProperty("option_color_contrast","0"));
        isEnableColorGamma = optionProperties.getProperty("option_color_is_gamma","false").compareTo("true")==0;
        colorGammaValue = Double.parseDouble(optionProperties.getProperty("option_color_gamma", "1.0"));
        isEnableColorInvert = optionProperties.getProperty("option_color_is_invert","false").compareTo("true")==0;
        isEnableFilterGray = optionProperties.getProperty("option_filter_is_gray","false").compareTo("true")==0;
        filterGrayThreshold = Integer.parseInt(optionProperties.getProperty("option_filter_gray","255"));


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
        optionProperties.setProperty("option_touch",""+touchOption);
        optionProperties.setProperty("option_volume_button",""+volumeButtonOption);
        optionProperties.setProperty("option_split",""+splitOption);
        optionProperties.setProperty("option_display",""+displayOption);
        optionProperties.setProperty("option_eink_clean",""+einkCleanOption);
        optionProperties.setProperty("option_resize_method", "" + resizeMethodOption);

        optionProperties.setProperty("option_color_is_brightness",(isEnableColorBrightness ? "true" : "false"));
        optionProperties.setProperty("option_color_brightness",""+colorBrightnessValue);
        optionProperties.setProperty("option_color_is_contrast",(isEnableColorContrast ? "true" : "false"));
        optionProperties.setProperty("option_color_contrast",""+colorContrastValue);
        optionProperties.setProperty("option_color_is_gamma",(isEnableColorGamma ? "true" : "false"));
        optionProperties.setProperty("option_color_gamma",""+colorGammaValue);
        optionProperties.setProperty("option_color_is_invert",(isEnableColorInvert ? "true" : "false"));
        optionProperties.setProperty("option_filter_is_gray",(isEnableFilterGray ? "true" : "false"));
        optionProperties.setProperty("option_filter_gray",""+filterGrayThreshold);

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
        if(isEnableColorBrightness || isEnableColorContrast || isEnableColorGamma || isEnableColorInvert )
            optionStr += FreeImageWrapper.FILTER_ADJUST_COLOR
                    + "|" + (isEnableColorBrightness ? colorBrightnessValue : "0")
                    + "|" + (isEnableColorContrast ? colorContrastValue : "0")
                    + "|" + (isEnableColorGamma ? colorGammaValue : "1.0")
                    + "|" + (isEnableColorInvert ? "1" : "0")
                    + " ";
        if(isEnableFilterGray)
            optionStr += FreeImageWrapper.FILTER_GRAY_2BIT + "|" + filterGrayThreshold
                    + " ";
        return optionStr;
    }

    public boolean isRightTouchNextPage()
    {
        switch (touchOption)
        {
            case Option.TOUCH_PREV_NEXT:
                return true;
            case Option.TOUCH_NEXT_PREV:
                return false;
        }
        return isReadLeftToRight;
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
    public int getTouchOption() { return touchOption; }
    public void setTouchOption(int option) { touchOption = option; }
    public int getVolumeButtonOption() { return volumeButtonOption; }
    public void setVolumeButtonOption(int option) { volumeButtonOption = option; }
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
    public int getEinkCleanOption()
    {
        return einkCleanOption;
    }
    public void setEinkCleanOption(int option)
    {
        einkCleanOption = option;
    }
    public int getResizeMethodOption()
    {
        return resizeMethodOption;
    }
    public void setResizeMethodOption(int option)
    {
        resizeMethodOption = option;
    }

    public boolean IsEnableColorBrightness() { return isEnableColorBrightness; }
    public void setEnableColorBrightness(boolean enable) { isEnableColorBrightness = enable; }
    public int getColorBrightnessValue() { return colorBrightnessValue; }
    public void setColorBrightnessValue(int threshold) { colorBrightnessValue = threshold; }

    public boolean IsEnableColorContrast() { return isEnableColorContrast; }
    public void setEnableColorContrast(boolean enable) { isEnableColorContrast = enable; }
    public int getColorContrastValue() { return colorContrastValue; }
    public void setColorContrastValue(int threshold) { colorContrastValue = threshold; }

    public boolean IsEnableColorGamma() { return isEnableColorGamma; }
    public void setEnableColorGamma(boolean enable) { isEnableColorGamma = enable; }
    public double getColorGammaValue() { return colorGammaValue; }
    public void setColorGammaValue(double threshold) { colorGammaValue = threshold; }

    public boolean IsEnableColorInvert() { return isEnableColorInvert; }
    public void setEnableColorInvert(boolean enable) { isEnableColorInvert = enable; }

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
