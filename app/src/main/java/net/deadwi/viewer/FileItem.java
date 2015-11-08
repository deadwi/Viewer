package net.deadwi.viewer;

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
    public String path;
    public String name;
    public String zipPath;
    public int type;
    public long size;

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

    public String getFullPath()
    {
        return getFullPath(path,name);
    }

    public static String getFullPath(String path, String name)
    {
        String fullPath = path;
        if(fullPath.endsWith("/")==false)
            fullPath += "/";
        fullPath += name;
        return fullPath;
    }

    private static int compareDir(FileItem lhs, FileItem rhs)
    {
        if(lhs.type==rhs.type)
            return 0;
        return lhs.type<rhs.type ? -1 : 1;
    }

    public static final Comparator CompareAlphIgnoreCase = new Comparator<FileItem>()
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

    public static final Comparator CompareFileSize = new Comparator<FileItem>()
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

    public static final Comparator CompareFileType = new Comparator<FileItem>()
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
