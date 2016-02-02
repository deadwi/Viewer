package net.deadwi.viewer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Bookmark {
    private static Bookmark ourInstance = new Bookmark();
    private String savePath;
    private SimpleDateFormat dataFormat;

    public static Bookmark getInstance() {
        return ourInstance;
    }

    private Bookmark() {
        dataFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public void setSavePath(String path)
    {
        savePath = path;
        if(savePath.charAt(savePath.length()-1)!='/')
            savePath += "/";
    }

    public ArrayList<BookmarkItem> loadBookmark(String dir)
    {
        //Log.d("BOOK","Load : "+getBookmarkPath(dir));
        ArrayList<BookmarkItem> items = new ArrayList<BookmarkItem>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(getBookmarkPath(dir)));
            String line;
            while ((line = br.readLine()) != null)
            {
                //Log.d("BOOK","Load line : "+line);
                BookmarkItem item = BookmarkItem.getBookmarkItem(line);
                if(item!=null)
                    items.add(item);
            }
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
        return items;
    }

    public ArrayList<BookmarkItem> loadBookmarkWithClean(String dir, ArrayList<FileItem> list)
    {
        ArrayList<BookmarkItem> bookmarkList = loadBookmark(dir);
        for(BookmarkItem item : bookmarkList)
        {
            boolean isExist = false;
            for(FileItem file : list)
            {
                if(item.filename.compareTo(file.name)==0)
                {
                    isExist = true;
                    break;
                }
            }
            if(isExist==false)
                bookmarkList.remove(item);
        }
        saveBookmark(dir, bookmarkList);

        return bookmarkList;
    }

    public void updateBookmark(String dir, BookmarkItem item)
    {
        item.lastReadDate = dataFormat.format(new Date());

        ArrayList<BookmarkItem> items = loadBookmark(dir);
        BookmarkItem mark = getBookmark(items, item.filename);
        if(mark!=null)
            mark.copyFrom(item);
        else
            items.add(item);
        saveBookmark(dir, items);
    }

    static BookmarkItem getBookmark(ArrayList<BookmarkItem> items, String filename)
    {
        for(BookmarkItem item : items)
        {
            if(item.filename.compareTo(filename)==0)
                return item;
        }
        return null;
    }

    private String getBookmarkPath(String dir)
    {
        return savePath+dir.replace('/','_')+".bookmark";
    }

    private void saveBookmark(String dir, ArrayList<BookmarkItem> items)
    {
        //Log.d("BOOK","Save : "+getBookmarkPath(dir));
        try
        {
            if(items.isEmpty())
            {
                FileManager.removeFile(getBookmarkPath(dir));
                return;
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(getBookmarkPath(dir)));
            for(BookmarkItem item : items)
            {
                //Log.d("BOOK","Save line : "+item.getLine());
                bw.write(item.getLine());
            }
            bw.flush();
            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}


