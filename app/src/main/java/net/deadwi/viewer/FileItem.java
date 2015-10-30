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
    public String path;
    public String name;
    public int type;
    public long size;

    public FileItem(String _path, String _name, int _type, long _size)
    {
        path=_path;
        name=_name;
        type=_type;
        size=_size;
    }

    private static int compareDir(FileItem lhs, FileItem rhs)
    {
        if(lhs.type==TYPE_DIR && rhs.type!=TYPE_DIR)
            return -1;
        else if(lhs.type!=TYPE_DIR && rhs.type==TYPE_DIR)
            return 1;
        return 0;
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
            return  lhs.size<rhs.size ? 1 : -1;
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
