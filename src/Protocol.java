import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.security.MessageDigest;

enum Command {
    LINKREQ("000"), FILELISTREQ("011"), SENDFILELIST("002"), FILEREQ("013"), FILESEND("004"), SENDEND("015"); // 第二位 1
    // 表示
    // 服务器端

    String cmd;

    Command(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }
}

class Protocol {
    int fileCount = 0;
    OutputStream os;
    InputStream is;
    String rootPath;
    ArrayList<File> childFiles;
    String md5s[], paths[];

    public static ArrayList<File> readDir(File root) {
        ArrayList<File> childDir = new ArrayList<>();
        File tmpChildDir[] = root.listFiles();
        for (File f : tmpChildDir) {
            childDir.add(f);
            if (f.isDirectory()) {
                childDir.addAll(readDir(f));
            }
        }
        return childDir;
    }

    public static ArrayList<File> deleteDir(ArrayList<File> childDir) {
        ArrayList<File> ChildFileDir = new ArrayList<File>();
        for (File f : childDir) {
            if (f.isFile()) {
                ChildFileDir.add(f);
            }
        }
        return ChildFileDir;
    }

    public Protocol(String rootPath, InputStream is) {
        this(rootPath);
        this.is = is;
    }

    public Protocol(String rootPath, OutputStream os) {
        this(rootPath);
        this.os = os;
    }

    public Protocol(String rootPath) {
        this.rootPath = rootPath; // 使用该构造函数将无法处理文件流
        childFiles = new ArrayList<File>();
    }

    public void setOs(OutputStream os) {
        this.os = os;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setIs(InputStream is) {
        this.is = is;
    }

    // 可能需要再加上每个文件的校验码
    String readCmd(String stream) throws IOException {
        String cmd = stream.substring(0, 3);
        // System.out.println(cmd);
        if (cmd.equals(Command.FILELISTREQ.getCmd())) {

            File root = new File(rootPath);
            childFiles = readDir(root);
            // childFiles=deleteDir(childFiles);
            if (childFiles.isEmpty()) {
                throw new NullPointerException("该文件夹下没有文件");
            } else {
                StringBuffer retString = new StringBuffer(Command.SENDFILELIST.getCmd());
                for (File chFile : childFiles) {
                    String tmpPath = chFile.getAbsolutePath();
                    tmpPath = tmpPath.substring(rootPath.length());
                    if (chFile.isDirectory()) {
                        retString.append(tmpPath + "/\n");
                        continue;
                    }
                    String md5 = MD5CalUtil.getMD5(chFile);
                    retString.append(tmpPath + "\n" + md5 + "\n");
                }
                return retString.toString();
            }
        }
        if (cmd.equals(Command.SENDFILELIST.getCmd())) {
            // 可能需要比较文件
            String infos[];// tmp in order to run;
            String info = stream.substring(3);
            infos = info.split("\n");
            int cntDirPath = 0, cntFilePath = 0;

            md5s = new String[infos.length];
            paths = new String[infos.length];
            int cnt = 0;
            for (int i = 0; i < infos.length; i++) {
                if (!infos[i].endsWith("/")) {
                    paths[cntFilePath] = infos[i];
                    md5s[cntFilePath] = infos[++i];
                    cntFilePath++;
                }
            }
            for (int i = 0; i < infos.length; i++) {
                if (infos[i].endsWith("/")) {
                    paths[cntFilePath + cntDirPath] = infos[i];
                    cntDirPath++;
                }
                // System.out.println(paths[i]);
            }
            // ++这里进行文件比较 留下需要更新的文件
            StringBuffer retString = new StringBuffer(Command.FILEREQ.getCmd());
            boolean isComplete = true;
            for (int i = 0; i < cntFilePath; i++) {
                String absolutePath = rootPath + paths[i];
                File checkFile = new File(absolutePath);
                if (!checkFile.exists()) {
                    isComplete = false;
                    retString.append(paths[i] + "\n");
                    childFiles.add(new File(absolutePath));
                    continue;
                }
                String md5 = MD5CalUtil.getMD5(checkFile);
                if (!md5.equals(md5s[i])) {
                    isComplete = false;
                    retString.append(paths[i] + "\n");
                    childFiles.add(new File(absolutePath));
                }
            }
            for (int i = cntFilePath; i < cntDirPath + cntFilePath; i++) {
                String absolutePath = rootPath + paths[i];
                File CheckFile = new File(absolutePath);
                if (!CheckFile.exists()) {
                    isComplete = false;
                    retString.append(paths[i] + "\n");
                }
            }
            if (!isComplete)
                return retString.toString();
            else
                return Command.SENDEND.getCmd();
            // 暂时不需要回应 发送完毕后 结束程序即可
            // 可能需要加入服务器端进行校验的信息 也可能需要中间进行校验
        }
        if (cmd.equals(Command.FILEREQ.getCmd())) {
            String path = stream.substring(3);
            String retString = Command.FILESEND.getCmd();
            String paths[]=path.split("\n");
            childFiles.clear();
            for(String p:paths){
                File f=new File(rootPath+p);
                childFiles.add(f);
            }
            // 需要在retString后面加入文件内容
            FileZipUtil.toZip(childFiles, os, rootPath);
            return retString;
        }
        if (cmd.equals(Command.FILESEND.getCmd())) {
            // 请读取到指令后外部处理接受流
            // 处理文件内容
            FileZipUtil.unZip(rootPath, is);
            return Command.SENDEND.getCmd();
        }
        return null; // 此处没有读到任何指令 传输一定出现了问题
    }
}

class MD5CalUtil {
    public static String getMD5(File file) {
        FileInputStream finStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            finStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = finStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            String md5Code = new BigInteger(1, MD5.digest()).toString(16);
            return md5Code;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (finStream != null)
                    finStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}