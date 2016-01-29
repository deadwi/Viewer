package net.deadwi.viewer;

import android.util.Log;

import java.io.File;
import java.util.Comparator;

/**
 * Created by jihun.jo on 2015-10-29.
 */
public class FileItem
{
    public static final int TYPE_DIR=0;
    public static final int TYPE_FILE=1;
    public static final int TYPE_DIR_IN_ZIP=2;
    public static final int TYPE_FILE_IN_ZIP=3;
    public static final int TYPE_NEW_SITE=1000;
    public static final int TYPE_SITE=1001;

    public String path;
    public String name;
    public String zipPath;
    public String date;
    public int type;
    public long size;
    public boolean checked=false;

    public FileItem(String _path, String _name, int _type, long _size)
    {
        path=_path;
        name=_name;
        type=_type;
        size=_size;
    }

    // for file in zip
    public FileItem(String _zipPath, String _path, long _size)
    {
        type = TYPE_FILE_IN_ZIP;
        if(_path.charAt(_path.length()-1)=='/')
        {
            type = TYPE_DIR_IN_ZIP;
            _path = _path.substring(0, _path.length()-1);
        }

        int pos = _path.lastIndexOf("/");
        if(pos>0)
        {
            path = _path.substring(0, pos);
            name = _path.substring(pos+1);
        }
        else
        {
            path = "";
            name = _path;
        }
        zipPath = _zipPath;
        size=_size;
    }

    public FileItem(String _path, String _name, String _date, int _type, long _size)
    {
        path=_path;
        name=_name;
        date=_date;
        type=_type;
        size=_size;
    }

    public String getFullPath()
    {
        return FileManager.getFullPath(path,name);
    }

    private static int compareDir(FileItem lhs, FileItem rhs)
    {
        if(lhs.type==rhs.type)
            return 0;
        return lhs.type<rhs.type ? -1 : 1;
    }

    private static int getIntFromString(String text,int start)
    {
        int i=start;
        for(;i<text.length();i++)
        {
            if(Character.isDigit(text.charAt(i))==false)
                break;
        }
        return Integer.parseInt(text.substring(start, i));
    }

    private static int compareNameWithNumber(String lhs, String rhs)
    {
        int i=0;
        int startNum=-1;
        int length = Math.min(lhs.length(),rhs.length());
        for(;i<length;i++)
        {
            if(startNum==-1 && Character.isDigit(lhs.charAt(i)) && Character.isDigit(rhs.charAt(i)))
                startNum = i;
            else if(lhs.charAt(i)==rhs.charAt(i))
                startNum = -1;
            if(lhs.charAt(i)!=rhs.charAt(i))
                break;
        }
        if(i==length || startNum==-1)
            return lhs.compareTo(rhs);

        try
        {
            int lhsInt = getIntFromString(lhs, startNum);
            int rhsInt = getIntFromString(rhs, startNum);
            if(lhsInt!=rhsInt)
                return lhsInt<rhsInt ? -1 : 1;
        }
        catch (NumberFormatException ex)
        {
        }
        return lhs.compareTo(rhs);
    }

    public static final Comparator<FileItem> CompareAlphIgnoreCase = new Comparator<FileItem>()
    {
        @Override
        public int compare(FileItem lhs, FileItem rhs)
        {
            int c = compareDir(lhs,rhs);
            if(c!=0)
                return c;

            return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
        }
    };

    public static final Comparator<FileItem> CompareAlphIgnoreCaseWithNumber = new Comparator<FileItem>()
    {
        @Override
        public int compare(FileItem lhs, FileItem rhs)
        {
            int c = compareDir(lhs,rhs);
            if(c!=0)
                return c;
            c = compareNameWithNumber(lhs.path.toLowerCase(), rhs.path.toLowerCase());
            if(c!=0)
                return c;
            return compareNameWithNumber(lhs.name.toLowerCase(), rhs.name.toLowerCase());
        }
    };

    public static final Comparator<FileItem> CompareFileSize = new Comparator<FileItem>()
    {
        @Override
        public int compare(FileItem lhs, FileItem rhs)
        {
            int c = compareDir(lhs,rhs);
            if(c!=0)
                return c;

            if(lhs.size==rhs.size)
                return 0;
            return  lhs.size<rhs.size ? -1 : 1;
        }
    };

    public static final Comparator<FileItem> CompareFileType = new Comparator<FileItem>()
    {
        @Override
        public int compare(FileItem lhs, FileItem rhs)
        {
            int c = compareDir(lhs,rhs);
            if(c!=0)
                return c;

            String ext;
            String ext2;
            int ret;

            try
            {
                ext = lhs.name.substring(lhs.name.lastIndexOf(".") + 1, lhs.name.length()).toLowerCase();
                ext2 = rhs.name.substring(rhs.name.lastIndexOf(".") + 1, rhs.name.length()).toLowerCase();
            }
            catch (IndexOutOfBoundsException e)
            {
                return 0;
            }
            ret = ext.compareTo(ext2);

            if (ret == 0)
                return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
            return ret;
        }
    };
}
