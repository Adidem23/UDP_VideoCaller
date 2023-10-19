package java_video_stream;

import com.github.sarxos.webcam.Webcam;
import com.sun.jna.Native;

import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

import com.sun.jna.NativeLibrary;
import com.sun.jna.platform.win32.WinUser.POINT;

import java.nio.file.Files;
import uk.co.caprica.vlcj.binding.LibVlc;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;
//import uk.co.caprica.vlcj.runtime.windows.WindowsRuntimeUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import static java_video_stream.JavaServer.getImage;

public class JavaServer {

	/**
	 * @param args
	 *            the command line arguments
	 */

	public static InetAddress[] inet;
	public static int[] port;

	public static InetAddress[] audinet;
	public static int[] audport;
	public static int i;
	static int count = 0;
	public static BufferedReader[] inFromClient;
	public static DataOutputStream[] outToClient;
	static Webcam webcam = Webcam.getDefault();
	
	public static void main(String[] args) throws Exception
	{
		webcam.open();
		JavaServer jv = new JavaServer();
	}

	public static BufferedImage getImage()
	{
		BufferedImage bm =webcam.getImage();
		return bm;
	}

	public JavaServer() throws Exception {
		
		
		NativeLibrary.addSearchPath("libvlc", "C:\\Program Files\\VideoLAN\\VLC");

		JavaServer.inet = new InetAddress[30];
		port = new int[30];

		JavaServer.audinet = new InetAddress[30];
		audport = new int[30];




		ServerSocket welcomeSocket = new ServerSocket(6782);
		System.out.println(welcomeSocket.isClosed());
		Socket connectionSocket[] = new Socket[30];
		inFromClient = new BufferedReader[30];
		outToClient = new DataOutputStream[30];

		DatagramSocket serv = new DatagramSocket(4321);
		DatagramSocket servaud=new DatagramSocket(54321);

		byte[] buf = new byte[62000];
		byte[] bufaud = new byte[62000];

		DatagramPacket dp = new DatagramPacket(buf, buf.length);
		DatagramPacket dpaud = new DatagramPacket(buf, buf.length);


		System.out.println("Gotcha");

		OutputStream[] os = new OutputStream[5];

		i = 0;

		

		while (true) {

			System.out.println(serv.getPort());
			serv.receive(dp);
			servaud.receive(dpaud);
			System.out.println(new String(dp.getData()));
			System.out.println(new String(dpaud.getData()));
			buf = "starts".getBytes();
			bufaud = "startsaudAdityaSuryawanshi".getBytes();

			inet[i] = dp.getAddress();
			port[i] = dp.getPort();

			audinet[i] = dpaud.getAddress();
			audport[i] = dpaud.getPort();

			DatagramPacket dsend = new DatagramPacket(buf, buf.length, inet[i], port[i]);
			serv.send(dsend);

			DatagramPacket dsendaud = new DatagramPacket(bufaud, bufaud.length, audinet[i], audport[i]);
			servaud.send(dsendaud);

			Vidthread sendvid = new Vidthread(serv);
			Audthread sendaud = new Audthread(servaud);

			System.out.println("waiting\n ");
			connectionSocket[i] = welcomeSocket.accept();
			System.out.println("connected " + i);

			inFromClient[i] = new BufferedReader(new InputStreamReader(connectionSocket[i].getInputStream()));
			outToClient[i] = new DataOutputStream(connectionSocket[i].getOutputStream());
			outToClient[i].writeBytes("Connected: from Server\n");

			System.out.println(inet[i]);
			sendvid.start();
			sendaud.start();

			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {

			}

			Thread SendServVideoThread = new Thread(() -> {
				SendServVideo ssv = new SendServVideo();
				try {
					ssv.main(new String[]{});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			SendServVideoThread.start();

			i++;

			if (i == 30) {
				break;
			}
		}
	}
}

class Vidthread extends Thread {

	int clientno;

	JFrame jf = new JFrame("Server Show");
	JLabel jleb = new JLabel();

	DatagramSocket soc;

	Robot rb = new Robot();

	byte[] outbuff = new byte[62000];

	BufferedImage mybuf;
	ImageIcon img;

	Rectangle rc;

	public Vidthread(DatagramSocket ds) throws Exception {
		soc = ds;
		System.out.println(soc.getPort());
		jf.setSize(500, 600);
		jf.setLocation(1000, 500);
		jf.setVisible(true);
	}

	public void run() {
		while (true) {
			try {

				int num = JavaServer.i;
				mybuf =getImage();
				img = new ImageIcon(mybuf);

				jleb.setIcon(img);
				jleb.setSize(25,25);
				LineBorder border = new LineBorder(Color.BLACK, 2);
				jleb.setBorder(border);
				jf.add(jleb);
				jf.repaint();
				jf.revalidate();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				ImageIO.write(mybuf, "jpg", baos);
				
				outbuff = baos.toByteArray();

				for (int j = 0; j < num; j++) {
					DatagramPacket dp = new DatagramPacket(outbuff, outbuff.length, JavaServer.inet[j],
							JavaServer.port[j]);
					soc.send(dp);
					baos.flush();
				}
				Thread.sleep(15);

			} catch (Exception e) {

			}
		}

	}

}
//AudThread
class Audthread extends Thread {

	int clientno;

	DatagramSocket socaud;

	byte[] outbuffaud = new byte[62000];

	BufferedImage mybuf;

	public Audthread(DatagramSocket dsaud) throws Exception {
		socaud = dsaud;
		System.out.println(socaud.getPort());
	}

	public void run() {
		while (true) {
			try {

				AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
				line.open(format);
				line.start();

				System.out.println("Server is sending audio...");
				while (true) {
					byte[] data = new byte[1024];
					int bytesRead = line.read(data, 0, data.length);
					System.out.println("Audio Server");
					DatagramPacket packet = new DatagramPacket(data, bytesRead, InetAddress.getByName("192.168.117.32"), 54321);
					socaud.send(packet);
					}
				} catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

		}

	}

}