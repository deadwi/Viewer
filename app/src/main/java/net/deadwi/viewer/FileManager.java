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
    private ArrayList<FileItem> recentFiles;

    public FileManager()
    {
        currentPath = "/";
    }

    public String getCurrentDir()
    {
        return currentPath;
    }

    public String getCurrentName()
    {
        String path = currentPath;
        if(path.isEmpty()==false && path.charAt(path.length() - 1)=='/')
            path = path.substring(0, path.length()-1);

        int pos = path.lastIndexOf("/");
        if(pos>=0)
            path = path.substring(pos+1);

        if(path.isEmpty()==true)
            return "/";
        return path;
    }

    public void setCurrentDir(String path)
    {
        currentPath = path;
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
            fileList = getFileListFromZipFile(currentPath,"/");
        else
            fileList = getFilelist(currentPath, isShowHiddenFiles);
        sortFilelist(fileList,sortType);
        recentFiles = fileList;
        return fileList;
    }

    public ArrayList<FileItem> getMatchFiles(String keyword)
    {
        Log.d("FILE", currentPath);
        ArrayList<FileItem> fileList;
        if(isDirectory(currentPath)==false && isZipFile(currentPath))
            fileList = getFileListFromZipFile(currentPath,"/");
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
            fileList = getFileListFromZipFile(path,"/");
        else
            fileList = getFilelist(path, isShowHiddenFiles);
        sortFilelist(fileList, sortType);
        return fileList;
    }

    public void movePreviousDir()
    {
        if(currentPath.compareTo("/")==0 || currentPath.isEmpty())
            currentPath="/";
        else
            currentPath = new File(currentPath).getParent();
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
        return String.format("%.1fG", size/1024.0/1024.0/1024.0);
    }

    static public String integerToIPAddress(int ip)
    {
        String ascii_address;
        int[] num = new int[4];

        num[0] = (ip & 0xff000000) >> 24;
        num[1] = (ip & 0x00ff0000) >> 16;
        num[2] = (ip & 0x0000ff00) >> 8;
        num[3] = ip & 0x000000ff;

        ascii_address = num[0] + "." + num[1] + "." + num[2] + "." + num[3];

        return ascii_address;
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

    static public void extractZipFilesFromDir(String zipName, String toDir, String fromDir)
    {
        if(!(toDir.charAt(toDir.length() - 1) == '/'))
            toDir += "/";
        if(!(fromDir.charAt(fromDir.length() - 1) == '/'))
            fromDir += "/";

        String org_path = fromDir + zipName;

        extractZipFiles(org_path, toDir);
    }

    static public ArrayList<FileItem> getFileListFromZipFile(String zipFile, String innerPath)
    {
        ArrayList<FileItem> fileList = (ArrayList<FileItem>)MinizipWrapper.getFilenamesInZip(zipFile);
        return fileList;
    }

    static public void extractZipFiles(String zip_file, String directory)
    {
        byte[] data = new byte[BUFFER];
        String name, path, zipDir;
        ZipEntry entry;
        ZipInputStream zipstream;

        if(!(directory.charAt(directory.length() - 1) == '/'))
            directory += "/";

        if(zip_file.contains("/")) {
            path = zip_file;
            name = path.substring(path.lastIndexOf("/") + 1,
                    path.length() - 4);
            zipDir = directory + name + "/";

        } else {
            path = directory + zip_file;
            name = path.substring(path.lastIndexOf("/") + 1,
                    path.length() - 4);
            zipDir = directory + name + "/";
        }

        new File(zipDir).mkdir();

        try {
            zipstream = new ZipInputStream(new FileInputStream(path));

            while((entry = zipstream.getNextEntry()) != null) {
                String buildDir = zipDir;
                String[] dirs = entry.getName().split("/");

                if(dirs != null && dirs.length > 0) {
                    for(int i = 0; i < dirs.length - 1; i++) {
                        buildDir += dirs[i] + "/";
                        new File(buildDir).mkdir();
                    }
                }

                int read = 0;
                FileOutputStream out = new FileOutputStream(
                        zipDir + entry.getName());
                while((read = zipstream.read(data, 0, BUFFER)) != -1)
                    out.write(data, 0, read);

                zipstream.closeEntry();
                out.close();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void createZipFile(String path)
    {
        File dir = new File(path);
        String[] list = dir.list();
        String name = path.substring(path.lastIndexOf("/"), path.length());
        String _path;

        if(!dir.canRead() || !dir.canWrite())
            return;

        int len = list.length;

        if(path.charAt(path.length() -1) != '/')
            _path = path + "/";
        else
            _path = path;

        try {
            ZipOutputStream zip_out = new ZipOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(_path + name + ".zip"), BUFFER));

            for (int i = 0; i < len; i++)
                zip_folder(new File(_path + list[i]), zip_out);

            zip_out.close();

        } catch (FileNotFoundException e) {
            Log.e("File not found", e.getMessage());

        } catch (IOException e) {
            Log.e("IOException", e.getMessage());
        }
    }

    static public int renameTarget(String filePath, String newName)
    {
        File src = new File(filePath);
        String ext = "";
        File dest;

        if(src.isFile())
			/*get file extension*/
            ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());

        if(newName.length() < 1)
            return -1;

        String temp = filePath.substring(0, filePath.lastIndexOf("/"));
        dest = new File(temp + "/" + newName + ext);
        if(src.renameTo(dest))
            return 0;
        return -1;
    }

    static public int createDir(String path, String name)
    {
        int len = path.length();

        if(len < 1 || len < 1)
            return -1;

        if(path.charAt(len - 1) != '/')
            path += "/";

        if (new File(path+name).mkdir())
            return 0;

        return -1;
    }

    static public int deleteTarget(String path)
    {
        File target = new File(path);

        if(target.exists() && target.isFile() && target.canWrite())
        {
            target.delete();
            return 0;
        }
        else if(target.exists() && target.isDirectory() && target.canRead())
        {
            String[] file_list = target.list();

            if(file_list != null && file_list.length == 0) {
                target.delete();
                return 0;

            } else if(file_list != null && file_list.length > 0) {

                for(int i = 0; i < file_list.length; i++) {
                    File temp_f = new File(target.getAbsolutePath() + "/" + file_list[i]);

                    if(temp_f.isDirectory())
                        deleteTarget(temp_f.getAbsolutePath());
                    else if(temp_f.isFile())
                        temp_f.delete();
                }
            }
            if(target.exists())
                if(target.delete())
                    return 0;
        }
        return -1;
    }

    static public ArrayList<String> searchInDirectory(String dir, String pathName)
    {
        ArrayList<String> names = new ArrayList<String>();
        search_file(dir, pathName, names);
        return names;
    }

    static public long getDirSize(String path)
    {
        Long size = new Long(0);
        get_dir_size(new File(path), size);
        return size;
    }

    static private void zip_folder(File file, ZipOutputStream zout) throws IOException
    {
        byte[] data = new byte[BUFFER];
        int read;

        if(file.isFile()){
            ZipEntry entry = new ZipEntry(file.getName());
            zout.putNextEntry(entry);
            BufferedInputStream instream = new BufferedInputStream(
                    new FileInputStream(file));

            while((read = instream.read(data, 0, BUFFER)) != -1)
                zout.write(data, 0, read);

            zout.closeEntry();
            instream.close();

        } else if (file.isDirectory()) {
            String[] list = file.list();
            int len = list.length;

            for(int i = 0; i < len; i++)
                zip_folder(new File(file.getPath() +"/"+ list[i]), zout);
        }
    }

    static private void get_dir_size(File path, Long dirSize)
    {
        File[] list = path.listFiles();
        int len;

        if(list != null) {
            len = list.length;

            for (int i = 0; i < len; i++) {
                try {
                    if(list[i].isFile() && list[i].canRead()) {
                        dirSize += list[i].length();

                    } else if(list[i].isDirectory() && list[i].canRead() && !isSymlink(list[i])) {
                        get_dir_size(list[i], dirSize);
                    }
                } catch(IOException e) {
                    Log.e("IOException", e.getMessage());
                }
            }
        }
    }

    // Inspired by org.apache.commons.io.FileUtils.isSymlink()
    static private boolean isSymlink(File file) throws IOException
    {
        File fileInCanonicalDir = null;
        if (file.getParent() == null)
        {
            fileInCanonicalDir = file;
        }
        else
        {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }
        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    /*
     * (non-JavaDoc)
     * I dont like this method, it needs to be rewritten. Its hacky in that
     * if you are searching in the root dir (/) then it is not going to be treated
     * as a recursive method so the user dosen't have to sit forever and wait.
     *
     * I will rewrite this ugly method.
     *
     * @param dir		directory to search in
     * @param fileName	filename that is being searched for
     * @param n			ArrayList to populate results
     */
    static private void search_file(String dir, String fileName, ArrayList<String> n)
    {
        File root_dir = new File(dir);
        String[] list = root_dir.list();

        if(list != null && root_dir.canRead()) {
            int len = list.length;

            for (int i = 0; i < len; i++) {
                File check = new File(dir + "/" + list[i]);
                String name = check.getName();

                if(check.isFile() && name.toLowerCase().
                        contains(fileName.toLowerCase())) {
                    n.add(check.getPath());
                }
                else if(check.isDirectory()) {
                    if(name.toLowerCase().contains(fileName.toLowerCase()))
                        n.add(check.getPath());

                    else if(check.canRead() && !dir.equals("/"))
                        search_file(check.getAbsolutePath(), fileName, n);
                }
            }
        }
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
