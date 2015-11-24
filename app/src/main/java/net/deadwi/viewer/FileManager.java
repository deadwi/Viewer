package net.deadwi.viewer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Stack;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.util.Log;

import net.deadwi.library.MinizipWrapper;

public class FileManager
{
    public static final int SORT_NONE = 0;
    public static final int SORT_ALPHA = 1;
    public static final int SORT_TYPE = 2;
    public static final int SORT_SIZE = 3;
    public static final int SORT_ALPHA_WITH_NUM = 4;
    private static final int BUFFER = 1024*4;

    private boolean isShowHiddenFiles = false;
    private int sortType = SORT_ALPHA_WITH_NUM;
    private String currentPath;
    private String innerPath;
    private ArrayList<FileItem> recentFiles;

    public FileManager()
    {
        currentPath = "/";
        innerPath = "";
    }

    public String getCurrentDir()
    {
        return currentPath;
    }

    public String getCurrentInnerDir()
    {
        return innerPath;
    }

    public String getCurrentName()
    {
        return getNameFromFullpath(currentPath);
    }

    public void setCurrentDir(String path)
    {
        currentPath = path;
        innerPath = "";
    }

    public void setCurrentDirInZip(String zipPath, String path)
    {
        currentPath = zipPath;
        innerPath = path;
    }

    public void setShowHiddenFiles(boolean choice)
    {
        isShowHiddenFiles = choice;
    }

    public void setSortType(int type)
    {
        sortType = type;
    }

    public ArrayList<FileItem> getRecentFiles()
    {
        return recentFiles;
    }

    public ArrayList<FileItem> getCurrentFiles()
    {
        Log.d("FILE", currentPath);
        ArrayList<FileItem> fileList;
        if(isDirectory(currentPath)==false && isZipFile(currentPath))
            fileList = getFileListFromZipFile(currentPath, innerPath);
        else
            fileList = getFilelist(currentPath, isShowHiddenFiles);
        sortFilelist(fileList, sortType);
        recentFiles = fileList;
        return fileList;
    }

    public ArrayList<FileItem> getMatchFiles(String keyword)
    {
        Log.d("FILE", currentPath);
        ArrayList<FileItem> fileList;
        if(isDirectory(currentPath)==false && isZipFile(currentPath))
            fileList = getFileListFromZipFile(currentPath, innerPath);
        else
            fileList = getFilelist(currentPath, isShowHiddenFiles);
        filterFilelist(fileList, keyword);
        sortFilelist(fileList,sortType);

        // no update recentFiles
        return fileList;
    }

    public ArrayList<FileItem> getFiles(String path)
    {
        ArrayList<FileItem> fileList;
        if(isDirectory(path)==false && isZipFile(path))
            fileList = getFileListFromZipFile(path,null);
        else
            fileList = getFilelist(path, isShowHiddenFiles);
        sortFilelist(fileList, sortType);
        return fileList;
    }

    public void movePreviousDir()
    {
        if(innerPath.compareTo("/")==0 || innerPath.isEmpty())
        {
            if (currentPath.compareTo("/") == 0 || currentPath.isEmpty())
                currentPath = "/";
            else
                currentPath = new File(currentPath).getParent();
        }
        else
            innerPath = getPathFromFullpath(innerPath, "");
    }

    public void moveNextDir(String name)
    {
        if(currentPath.isEmpty() || (currentPath.charAt(currentPath.length()-1)!='/' && currentPath.charAt(currentPath.length()-1)!='\\'))
            currentPath += "/";
        currentPath += name;
    }

    static public boolean isDirectory(String path)
    {
        return new File(path).isDirectory();
    }

    static public String getFileSizeText(long size)
    {
        if(size<1000)
            return size+"B";
        else if(size/1024<1000)
            return String.format("%.1fK", size/1024.0);
        else if(size/1024/1024<1000)
            return String.format("%.1fM", size/1024.0/1024.0);
        return String.format("%.1fG", size / 1024.0 / 1024.0 / 1024.0);
    }

    public static String getFullPath(String path, String name)
    {
        String fullPath = path;
        if(fullPath.endsWith("/")==false)
            fullPath += "/";
        fullPath += name;
        return fullPath;
    }

    static public String getNameFromFullpath(String path)
    {
        if(path.isEmpty()==false && path.charAt(path.length() - 1)=='/')
            path = path.substring(0, path.length()-1);

        int pos = path.lastIndexOf("/");
        if(pos>=0)
            path = path.substring(pos+1);

        if(path.isEmpty()==true)
            return "/";
        return path;
    }

    static public String getPathFromFullpath(String path, String defaultPath)
    {
        if(path.isEmpty()==false && path.charAt(path.length() - 1)=='/')
            return path;

        int pos = path.lastIndexOf("/");
        if(pos>=0)
            path = path.substring(0, pos);
        else
            return defaultPath;
        return path;
    }

    static public boolean isZipFile(String path)
    {
        return path.toLowerCase().endsWith(".zip");
    }
    static public boolean isImageFile(String path)
    {
        String lowerString = path.toLowerCase();

        return lowerString.endsWith(".jpg") ||
                lowerString.endsWith(".jpeg") ||
                lowerString.endsWith(".png") ||
                lowerString.endsWith(".bmp") ||
                lowerString.endsWith(".gif");
    }

    static public ArrayList<FileItem> getFileListFromZipFile(String zipFile, String innerPath)
    {
        ArrayList<FileItem> fileList = (ArrayList<FileItem>)MinizipWrapper.getFilenamesInZip(zipFile);
        if(innerPath==null)
            return fileList;
        String innerPathPrefix = innerPath + "/";

        Log.d("ZIP", "innerPath = "+innerPath);
        ArrayList<FileItem> newList = new ArrayList<FileItem>();
        for(FileItem item : fileList)
        {
            if(item.path.compareTo(innerPath)==0)
                newList.add(item);
            // two level dir not exist in file list
            else if(item.path.startsWith(innerPathPrefix))
            {
                int pos = item.path.indexOf("/",innerPathPrefix.length());
                String newPath = item.path.substring(0, pos < 0 ? item.path.length() : pos);
                if(newList.isEmpty()==true || newList.get(newList.size()-1).getFullPath().compareTo(newPath)!=0)
                    newList.add( new FileItem(item.zipPath, newPath+"/", 0) );
            }
        }
        return newList;
    }

    static private ArrayList<FileItem> getFilelist(String path, boolean isShowHiddenFiles)
    {
        ArrayList<FileItem> fileList = new ArrayList<FileItem>();
        File pathFile = new File(path);

        if(pathFile.exists() && pathFile.canRead())
        {
            String[] list = pathFile.list();

            for (int i = 0; i < list.length; i++)
            {
                if(!isShowHiddenFiles && list[i].charAt(0) == '.')
                    continue;

                File file = new File(path + "/" + list[i]);
                fileList.add(new FileItem(
                        path,
                        list[i],
                        file.isDirectory() ? FileItem.TYPE_DIR : FileItem.TYPE_FILE,
                        file.length()
                ));
            }
        }
        return fileList;
    }

    static private void filterFilelist(ArrayList<FileItem> fileList, String _keyword)
    {
        String keyword = _keyword.toLowerCase();
        Iterator<FileItem> iter = fileList.iterator();
        while (iter.hasNext())
        {
            FileItem item = iter.next();
            if(item.name.toLowerCase().contains(keyword)==false)
                iter.remove();
        }
    }

    static private void sortFilelist(ArrayList<FileItem> fileList, int sortType)
    {
        switch(sortType)
        {
            case SORT_NONE:
                break;

            case SORT_ALPHA:
                Collections.sort(fileList, FileItem.CompareAlphIgnoreCase);
                break;

            case SORT_SIZE:
                Collections.sort(fileList, FileItem.CompareFileSize);
                break;

            case SORT_TYPE:
                Collections.sort(fileList, FileItem.CompareFileType);
                break;

            case SORT_ALPHA_WITH_NUM:
                Collections.sort(fileList, FileItem.CompareAlphIgnoreCaseWithNumber);
                break;
        }
    }
}
