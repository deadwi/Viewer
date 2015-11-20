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
    private static final int BUFFER = 1024*4;

    private boolean isShowHiddenFiles = false;
    private int sortType = SORT_ALPHA;
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

    static public int copyToDirectory(String old, String newDir)
    {
        File old_file = new File(old);
        File temp_dir = new File(newDir);
        byte[] data = new byte[BUFFER];
        int read = 0;

        if(old_file.isFile() && temp_dir.isDirectory() && temp_dir.canWrite()){
            String file_name = old.substring(old.lastIndexOf("/"), old.length());
            File cp_file = new File(newDir + file_name);

            try {
                BufferedOutputStream o_stream = new BufferedOutputStream(
                        new FileOutputStream(cp_file));
                BufferedInputStream i_stream = new BufferedInputStream(
                        new FileInputStream(old_file));

                while((read = i_stream.read(data, 0, BUFFER)) != -1)
                    o_stream.write(data, 0, read);

                o_stream.flush();
                i_stream.close();
                o_stream.close();

            } catch (FileNotFoundException e) {
                Log.e("FileNotFoundException", e.getMessage());
                return -1;

            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
                return -1;
            }

        }else if(old_file.isDirectory() && temp_dir.isDirectory() && temp_dir.canWrite()) {
            String files[] = old_file.list();
            String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());

            if(!new File(dir).mkdir())
                return -1;
            for (String file : files)
                copyToDirectory(old + "/" + file, dir);

        } else if(!temp_dir.canWrite())
            return -1;

        return 0;
    }

    static public boolean isZipFile(String path)
    {
        return path.toLowerCase().endsWith(".zip");
    }
    static public boolean isImageFile(String path)
    {
        String lowerString = path.toLowerCase();

        return lowerString.endsWith(".jpg") ||
                lowerString.endsWith(".png") ||
                lowerString.endsWith(".gif");
    }

    static public ArrayList<FileItem> getFileListFromZipFile(String zipFile, String innerPath)
    {
        ArrayList<FileItem> fileList = (ArrayList<FileItem>)MinizipWrapper.getFilenamesInZip(zipFile);
        if(innerPath==null)
            return fileList;

        Log.d("ZIP", "innerPath = "+innerPath);
        ArrayList<FileItem> newList = new ArrayList<FileItem>();
        for(FileItem item : fileList)
        {
            Log.d("ZIP",item.path+" "+item.name);
            if(item.path.compareTo(innerPath)==0)
                newList.add(item);
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
        }
    }
}
