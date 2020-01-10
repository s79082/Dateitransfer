//package rnBeleg;

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

//import rnBeleg.ServerUdp;
//import static ServerUdp;

public class ClientUdp {

    private static final double LOSS_RATE = 0.0;
    private static final int AVERAGE_DELAY = 100; // milliseconds
    private static long returnTime;
    private static int returnSeq;
    private static final int SOCKET_TIMEOUT = 2000; 


    //public void main(String file_name, String host_adress, int host_port) throws Exception {
    public static void main(String[] args) throws Exception {
        String filename = args[0];
        //String filename = file_name;
	    String filepath = "U:/RN/rnBeleg/";

        //int port = host_port;
        int port = 3330;
        //String host = host_adress;
        
        String host = "idefix.informatik.htw-dresden.de";

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(SOCKET_TIMEOUT);
        InetAddress serverAddress = InetAddress.getByName(host);
        Checksum crc = new CRC32();
        long checksum;
      
        // byte[] buffer = new byte[1024];

        // setting up input stream
        //File file = new File("U:\\RN\\rnBeleg\\" + filename);
        File file = new File(filepath+ filename);

        FileInputStream fis = new FileInputStream(file);

        long fileLength = file.length();
        System.out.println("filelegth: " + fileLength);
        short fileNameLength = (short) filename.length();

        // send start packet
        StartPackage sp = new StartPackage((byte) 0, filename.getBytes(), fileLength);

        ServerUdp.printArray(sp.getBytes());

        crc.reset();

        System.out.println(sp.calculateChecksum());
        //ServerUdp.printArray(sp.calculateChecksum().getBytes());
        
        //sp.send(socket, serverAddress, port);

        send_check(sp, socket, serverAddress, port, (byte) 0);
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
            

            System.out.println("bytes read: "+read);
            System.out.println("bytes remaining: "+remaining_bytes);
            // allocate max PAYLOAD_SIZE bytes
            byte[] remaining_data = new byte[read];
            remaining_data = Arrays.copyOfRange(buffer, 0, read);
            //ByteBuffer tmp_buff = ByteBuffer.wrap(remaining_data);

            crc.update(remaining_data, 0, remaining_data.length);

            //ServerUdp.printArray(remaining_data);

            lastPackage = (remaining_bytes == 0);
            dp = new DataPackage(packageId, remaining_data, lastPackage);

            if(lastPackage) {
                // append crc
                dp.setChecksum((int) crc.getValue());
                System.out.println("CRC Client: "+ ((int) crc.getValue()));
            }
            
            ServerUdp.printArray(dp.getBytes());
            
            // try 10 times 
            if(!send_check(dp, socket, serverAddress, port, packageId))
                exit_msg("Last try receiving correct ACK failed. Aborting");

            System.out.println("bytes send: "+(fileLength - remaining_bytes));
            
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

        int n_try = 0, received_pid;
        DatagramPacket response = new DatagramPacket(new byte[3], 3);

        while (n_try <= 9)
        {
            pack.send(socket, adress, port);
            
            try 
            {
                socket.receive(response);
            }
            catch (SocketTimeoutException e)
            {
                System.out.println("try nr "+(n_try + 1)+ " failed: received socket timeout (" + SOCKET_TIMEOUT+"ms)");
                n_try++;

                // jump to next try
                continue;
            }
            // ack is last byte 
            if ((received_pid = response.getData()[2]) == pid) 
            {
                // correct ACK was received
                System.out.println("received correct ACK");
                return true;
            }
            else
            {
                System.out.println("try nr "+(n_try + 1)+ " failed: received incorrect ACK (expected: " + pid + ", received: "+received_pid + ")");
                n_try++;
            }
        }

        if (n_try >= 9)
            return false;
        return true;

/*
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
        */
    }
}
   
