package java_video_stream;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.io.IOException;
import java.net.*;


public class SendServVideo {
    public static DatagramSocket ds;
    public static DatagramSocket dsaud;

    public static void main(String[] args) throws Exception {
        String receiverIP = "192.168.233.32";
        ds = new DatagramSocket();
        dsaud = new DatagramSocket();

        byte[] init = new byte[62000];
        init = "givedata".getBytes();

        InetAddress addr = InetAddress.getByName(receiverIP);

        DatagramPacket dp = new DatagramPacket(init,init.length,addr,1234);
        DatagramPacket dpaud = new DatagramPacket(init,init.length,addr,12345);
        System.out.println("Packet Send");

        ds.send(dp);
        dsaud.send(dpaud);

        DatagramPacket rcv = new DatagramPacket(init, init.length);
        DatagramPacket rcvaud = new DatagramPacket(init, init.length);

        ds.receive(rcv);
        System.out.println("Packet Received");
        dsaud.receive(rcvaud);
        System.out.println(new String(rcv.getData()));

        System.out.println(ds.getPort());

        System.out.println(new String(rcvaud.getData()));

        System.out.println(dsaud.getPort());
        VidServshow vd = new VidServshow();
        AudServshow ad = new AudServshow();
        ad.start();
        vd.start();

        String modifiedSentence;

        InetAddress inetAddress = InetAddress.getByName(receiverIP);
        System.out.println(inetAddress);

        Socket clientSocket = new Socket(inetAddress, 9876);
        DataOutputStream outToServer =
                new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes("Thanks man\n");
        clientSocket.close();
    }
}

class VidServshow extends Thread {

    JFrame jf = new JFrame();
    public static JPanel jp = new JPanel(new GridLayout(2,1));
    public static JPanel half = new JPanel(new GridLayout(3,1));
    JLabel jl = new JLabel();

    byte[] rcvbyte = new byte[62000];

    DatagramPacket dp = new DatagramPacket(rcvbyte, rcvbyte.length);
    BufferedImage bf;
    ImageIcon imc;


    public VidServshow() throws Exception {
        jf.setSize(200, 200);
        jf.setTitle("Client Show");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setAlwaysOnTop(true);
        jf.setLayout(new BorderLayout());
        jf.setVisible(true);
        jp.add(jl);
        jf.add(jp);


        JScrollPane jpane = new JScrollPane();
        jpane.setSize(300, 200);
        jpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    }

    @Override
    public void run() {

        try {
            System.out.println("got in");
            do {
                System.out.println("doing");
                System.out.println(SendServVideo.ds.getPort());

                SendServVideo.ds.receive(dp);
                System.out.println("received");
                ByteArrayInputStream bais = new ByteArrayInputStream(rcvbyte);

                bf = ImageIO.read(bais);

                if (bf != null) {
                    imc = new ImageIcon(bf);
                    jl.setIcon(imc);
                    Thread.sleep(15);
                }
                jf.revalidate();
                jf.repaint();


            } while (true);

        } catch (Exception e) {
            System.out.println("couldnt do it");
        }
    }
}

class AudServshow extends Thread {

    byte[] rcvbyte = new byte[62000];
    DatagramPacket dpaud = new DatagramPacket(rcvbyte, rcvbyte.length);

    public AudServshow() throws Exception {

    }

    @Override
    public void run() {

        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            System.out.println("Client is receiving audio...");
            while (true) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                SendServVideo.dsaud.receive(packet);
                line.write(data, 0, data.length);
            }
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }
}