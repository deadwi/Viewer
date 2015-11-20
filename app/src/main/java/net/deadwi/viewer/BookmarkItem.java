package net.deadwi.viewer;

/**
 * Created by jihun.jo on 2015-11-20.
 */
public class BookmarkItem
{
    public String filename;
    public String innerName;
    public int fileIndex;
    public int fileCount;
    public int viewIndex;
    public String lastReadDate;

    static public BookmarkItem getBookmarkItem(String line)
    {
        String[] tokens = line.split("\t");
        if(tokens.length!=6)
            return null;
        BookmarkItem item = new BookmarkItem();
        item.filename = tokens[0];
        item.innerName = tokens[1];
        item.fileIndex = Integer.parseInt(tokens[2]);
        item.fileCount = Integer.parseInt(tokens[3]);
        item.viewIndex = Integer.parseInt(tokens[4]);
        item.lastReadDate = tokens[5];
        return item;
    }

    public BookmarkItem()
    {

    }

    public String getLine()
    {
        return filename+"\t"+innerName+"\t"+fileIndex+"\t"+fileCount+"\t"+viewIndex+"\t"+lastReadDate+"\n";
    }

    public void copyFrom(BookmarkItem obj)
    {
        filename = obj.filename;
        innerName = obj.innerName;
        fileIndex = obj.fileIndex;
        fileCount = obj.fileCount;
        viewIndex = obj.viewIndex;
        lastReadDate = obj.lastReadDate;
    }
}
