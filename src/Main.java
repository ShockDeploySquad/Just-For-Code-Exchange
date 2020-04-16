import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Main {

	public static void main(String[] args) throws Exception {

		String path = "Z:\\计网\\课程项目二\\ProtocolTest-master\\srcfile\\";
		String outPath = "Z:\\计网\\课程项目二\\ProtocolTest-master\\destfile\\";
		FileOutputStream fos = new FileOutputStream("1.zip");
		Protocol protocolTest = new Protocol(path, fos), protocolTest2 = new Protocol(outPath);

		String s = protocolTest.readCmd("011");
		System.out.println(s);
		s = protocolTest2.readCmd(s);
		System.out.println(s);
		s = protocolTest.readCmd(s);
		FileInputStream fis = new FileInputStream("1.zip");
		protocolTest2.setIs(fis);
		System.out.println(s);
		s = protocolTest2.readCmd(s);
		System.out.println(s);
		fos.close();
	}
}
