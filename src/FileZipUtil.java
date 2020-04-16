import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileZipUtil {
	public static void toZip(ArrayList<File> srcFiles, OutputStream out, String inPath) throws RuntimeException {
		System.out.println("开始生成zip流");
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(out);
			for (File srcFile : srcFiles) {
				String childPath = srcFile.getAbsolutePath().substring(inPath.length());
				System.out.println(childPath);
				if (srcFile.isFile()) {
					byte[] buff = new byte[8192];
					zos.putNextEntry(new ZipEntry(childPath));
					int len;
					FileInputStream in = new FileInputStream(srcFile);
					while ((len = in.read(buff)) != -1) {
						zos.write(buff, 0, len);
					}
					zos.closeEntry();
					in.close();
				} else {
					zos.putNextEntry(new ZipEntry(childPath + "/"));
					// zos.closeEntry();
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("zip error", ex);
		} finally {
			if (zos != null) {
				try {
					zos.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		System.out.println("zip流结束");
	}

	public static void createDir(File file) {
		File parentFile = file.getParentFile();
		if (parentFile != null) {
			parentFile.mkdirs();
		}
	}

	public static void unZip(String outPath, InputStream is) throws IOException {
        ZipInputStream zis=new ZipInputStream(is);
        ZipEntry nextEntry=zis.getNextEntry();
        while(nextEntry!=null){
            String name=nextEntry.getName();
            //System.out.println(name);

            File file=new File(outPath+name);
            if(name.endsWith("/")){
                file.mkdirs();
            }
            else {
            	file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int len;
                byte[] buff = new byte[8192];
                while ((len = zis.read(buff)) != -1) {
                    bos.write(buff, 0, len);
                }
                bos.close();
                fos.close();
                zis.closeEntry();
            }
            nextEntry=zis.getNextEntry();
        }
    }
}
