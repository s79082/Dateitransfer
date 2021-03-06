//package rnBeleg;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.io.File;
import java.nio.file.FileSystems;
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
	    String filepath = "/user/profile/active/ia18/s79082/RN/rnBeleg/";

        //int port = 3333;
        int port = 1024;
        //String host = host_adress;
        
        //String host = "idefix.informatik.htw-dresden.de";
        String host = "localhost";
        System.out.println(FileSystems.getDefault().getPath("test1.txt").toAbsolutePath().toString());
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(SOCKET_TIMEOUT);
        InetAddress serverAddress = InetAddress.getByName(host);
        Checksum crc = new CRC32();      

        // setting up input stream
        File file = new File(filepath + filename);
        FileInputStream fis = new FileInputStream(file);
        long fileLength = file.length();
        short fileNameLength = (short) filename.length();
        System.out.println("filelegth: " + fileLength);
        
        // create start package
        // start package always has pid 0
        StartPackage sp = new StartPackage((byte) 0, filename.getBytes(), fileLength);
        
        ServerUdp.printArray(sp.getBytes());

        crc.reset();
        
        // send start packet
        send_check(sp, socket, serverAddress, port, (byte) 0);

        byte[] buffer = new byte[DataPackage.PAYLOAD_SIZE];

        // the StartPackage.packageId is 0 so the first DataPackage has 1
        byte packageId = (byte) 1;
        boolean lastPackage;
        
        // bytes read, max payload size many
        int read;

        // counts the bytes still to be read from file
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

            crc.update(remaining_data, 0, remaining_data.length);

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
                System.out.println("try nr "+(n_try + 1)+ " failed: received incorrect ACK (expected: " + pid + ", received: "+ received_pid + ")");
                n_try++;
            }
        }

        return false;

    }
}
   
