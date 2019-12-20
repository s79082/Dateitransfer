package rnBeleg;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.io.File;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.ByteBuffer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import rnBeleg.ServerUdp;

public class ClientUdp {

    private static final double LOSS_RATE = 0.0;
    private static final int AVERAGE_DELAY = 100; // milliseconds
    private static long returnTime;
    private static int returnSeq;


    public static void main(String[] args) throws Exception {
        String filename = "test4.txt";

        int port = 1024;
        String host = "localhost";

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(1000);
        InetAddress serverAddress = InetAddress.getByName(host);
        Checksum crc = new CRC32();
        long checksum;
      
        // byte[] buffer = new byte[1024];

        // setting up input stream
        //File file = new File("U:\\RN\\rnBeleg\\" + filename);
        File file = new File("/user/profile/active/ia18/s79082/RN/rnBeleg/	" + filename);

        FileInputStream fis = new FileInputStream(file);

        long fileLength = file.length();
        System.out.println("filelegth: " + fileLength);
        short fileNameLength = (short) filename.length();

        // send start packet
        StartPackage sp = new StartPackage((byte) 1, filename.getBytes(), fileLength);
        
        sp.send(socket, serverAddress, port);

        
        //ByteBuffer buff_alldata = ByteBuffer.allocate(DataPackage.PAYLOAD_SIZE);

        // reading the file into buffer

        // the amount of bytes still unused in buffer
        //int remaining = buffer.length;

        byte[] buffer = new byte[DataPackage.PAYLOAD_SIZE];

        // the StartPackage.packageId is 0 so the first DataPackage has 1
        byte packageId = (byte) 1;
        boolean lastPackage;
        int read;
        long remaining_bytes = fileLength;

        DataPackage dp;

        while((read = fis.read(buffer, 0, DataPackage.PAYLOAD_SIZE)) > -1)
        {
            remaining_bytes -= read;
            
            // allocate max PAYLOAD_SIZE bytes
            byte[] remaining_data = new byte[read];
            remaining_data = Arrays.copyOfRange(buffer, 0, read);
            //ByteBuffer tmp_buff = ByteBuffer.wrap(remaining_data);

            crc.update(remaining_data);

            //ServerUdp.printArray(remaining_data);

            lastPackage = (remaining_bytes == 0);
            dp = new DataPackage(packageId, remaining_data, lastPackage);

            if(lastPackage) {
                // append crc
                dp.setChecksum((int) crc.getValue());
                

            }
            System.out.println("CRC Client: "+ ((int) crc.getValue()));
            ServerUdp.printArray(dp.getBytes());
            
            // try 10 times 
            if(!send_check(dp, socket, serverAddress, port, packageId))
                exit_msg("Last try receiving correct ACK failed. Aborting");
            
            // update packageId
            packageId++;
            packageId %= 2;
            
        }

    }

    public static void exit_msg(String msg) 
    {
            System.out.println(msg);
            System.exit(1);    
    }

    // sends pack via socket to adress and port. Waits for a ConfirmationPackage and checks ACK with pid. Repeat 10 times
    public static boolean send_check(UdpPackage pack, DatagramSocket socket, InetAddress adress, int port, byte pid) throws Exception
    {
        int i;
        for (i = 1; i <= 10; i++) 
        {
            pack.send(socket, adress, port);
            // the ACK package is 3 bytes long
            DatagramPacket response = new DatagramPacket(new byte[3], 3);
            socket.receive(response);
            System.out.println("Try: "+i);

            // 1 byte ACK comes comes after 2 byte sessionId
            if (response.getData()[2] == pid) 
            {
                // correct ACK was received
                System.out.println("received correct ACK");
                return true;
            }
        }
        return false;    
    }
}
   
