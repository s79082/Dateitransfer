//package rnBeleg;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.lang.model.util.ElementScanner6;

/*
 * Server to process ping requests over UDP.
 */
public class ServerUdp
{
   private static final double LOSS_RATE = 0.3;
   private static final int AVERAGE_DELAY = 100;  // milliseconds
   public static final long DATAPACKAGE_HEADER_SIZE = 3;

   public static void main(String[] args) throws Exception
   {

      int port = 1024;
      long fileLength;
      long actual_fileLegth = 0;
      String file_name;
      int file_name_length;
      ByteBuffer data_buf;

      // We expect a StartPackage first
      // no futher processing without 
      DatagramSocket socket = new DatagramSocket(port);
      DatagramPacket request = new DatagramPacket(new byte[561], 561);
      String supposedStartToken;
     
      do{

         socket.receive(request);

         // the token START is at position 3 five bytes long
         supposedStartToken = new String(Arrays.copyOfRange(request.getData(), 3, 3 + 5), "UTF-8");

      }
      // repeat until startpackage received
      while(!supposedStartToken.equals(StartPackage.marker));
      System.out.println("Start package received!");
      System.out.println("Start package bytes:");
      printArray(request.getData());

      // calculate client data
      InetAddress clientHost = request.getAddress();
      int clientPort = request.getPort();

      data_buf = ByteBuffer.wrap(request.getData());

      fileLength = data_buf.getLong(8);

      System.out.println("filelength: " + fileLength);

      long remaining_bytes = fileLength;

      file_name_length = data_buf.getChar(16);

      System.out.println("filenamelength: " + file_name_length);
      // ack start package
      ConfirmationPackage confirm = new ConfirmationPackage((byte) 0);
      confirm.send(socket, clientHost, clientPort);

      Checksum crc = new CRC32();
      crc.update(ByteBuffer.wrap(Arrays.copyOfRange(request.getData(), 0, Short.BYTES + Byte.BYTES + StartPackage.marker.length() 
         + Long.BYTES + file_name_length)));
      printArray(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());

      crc.reset();

      

      // copy file name
      byte[] file_name_bytes = new byte[file_name_length];
      for (int idx = 18; idx < (18 + file_name_length); idx++){
         file_name_bytes[idx - 18] = data_buf.get(idx);
      }
      //data_buf.get(file_name_bytes, 0, file_name_length);

      file_name = new String(file_name_bytes, "UTF-8");
      String new_file_name = "/user/profile/active/ia18/s79082/RN/rnBeleg/" + file_name.split("\\.")[0] + " 1.txt";
      System.out.println(new_file_name);
      File file = new File(new_file_name);
      file.createNewFile();
      FileOutputStream output = new FileOutputStream(file);
      System.out.println("filename: " + file_name);

      boolean lastPackage = false;
      boolean CRCcorrect = false;
      
      byte expected_pid = (byte) 1;

      // Processing loop.
      while(true){

         // the payload size
         long datasize;
         // the size of the whole package
         int datagramm_size = 0;

         if (remaining_bytes <= DataPackage.PAYLOAD_SIZE)
         {
            // if the incoming data has equal or less bytes 
            // the receiving datagramm should also only hold this amount of bytes
            datasize = remaining_bytes;

            // this is the last Package to be received. It will have an 4 byte CRC
            datagramm_size += 4;
            lastPackage = true;

            System.out.println("last package received");

         }
         else
            datasize =  DataPackage.PAYLOAD_SIZE;

         datagramm_size += (int)(DATAPACKAGE_HEADER_SIZE + datasize);

         request = new DatagramPacket(new byte[datagramm_size], datagramm_size);
         socket.receive(request);
         printArray(request.getData());
         // before any further processing, we check the pid and sid of the data package
         byte pid = request.getData()[2];
         byte send_ack;
         if (pid != expected_pid)
         {
            System.out.println("received package id out of order (received: "+pid+", expected: "+expected_pid+")");
            send_ack = pid;    // send the 'wrong' PID as ACK
            // jump to next iteration (SW protocol)
            // remaining_bytes will not be updated before a correct pid and sid are received
            continue;   
         }
            
         else
            send_ack = expected_pid;

         remaining_bytes -= datasize;

         
         actual_fileLegth +=datasize;
         byte[] tmp = request.getData();

         // last parameter is exclusive
         byte[] data = Arrays.copyOfRange(tmp, (int) DATAPACKAGE_HEADER_SIZE, (int) (DATAPACKAGE_HEADER_SIZE + datasize));
         
         // update the crc with the data array
         crc.update(data, 0, data.length);

         if (lastPackage){
            byte[] receivedCrc = Arrays.copyOfRange(tmp, (int)(DATAPACKAGE_HEADER_SIZE + datasize), (int)(datagramm_size));
            ByteBuffer tmp_buff = ByteBuffer.wrap(receivedCrc);
            CRCcorrect = (tmp_buff.getInt() == (int) crc.getValue());
            printArray(receivedCrc);   
            System.out.println(CRCcorrect);
            if (!CRCcorrect)
               ClientUdp.exit_msg("wrong crc in last data package");
            System.out.println("CRC Server: "+((int) crc.getValue()));
         }
         
            
         // send ACK
         ConfirmationPackage cp = new ConfirmationPackage(send_ack);
         System.out.println("received bytes: "+ actual_fileLegth);
         System.out.println("data: ");
         printArray(data);
         
            
         DatagramPacket response = new DatagramPacket(cp.getBytes(), cp.getBytes().length, clientHost, clientPort);
         //cp.send(socket, InetAddress.getByName("localhost"), port);
         socket.send(response);
         
         // update next expected package id
         expected_pid++;
         expected_pid %= 2;

         output.write(data);

         if (lastPackage)
            break;
      }
      output.close();
      socket.close();
      ClientUdp.exit_msg("File transfer complete");
   }
   
   // helper to print byte arrays
   public static void printArray(byte[] bytes) throws UnsupportedEncodingException
   {
      System.out.println(Arrays.toString(bytes));
   }

}
