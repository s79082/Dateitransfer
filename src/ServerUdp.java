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

      Checksum crc = new CRC32();
      crc.update(ByteBuffer.wrap(Arrays.copyOfRange(request.getData(), 0, request.getData().length - Integer.BYTES)));
      printArray(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());

      crc.reset();

      data_buf = ByteBuffer.wrap(request.getData());

      //fileLength = ByteBuffer.wrap(Arrays.copyOfRange(request.getData(), 8, 8 + 8)).getLong();

      fileLength = data_buf.getLong(8);

      System.out.println("filelength: " + fileLength);

      long remaining_bytes = fileLength;

      //file_name_length = ByteBuffer.wrap(Arrays.copyOfRange(request.getData(), 16, 16 + 2)).getChar();
      file_name_length = data_buf.getChar(16);

      System.out.println("filenamelength: " + file_name_length);

      byte[] file_name_bytes = new byte[file_name_length];

      for (int idx = 18; idx < (18 + file_name_length); idx++){
         file_name_bytes[idx - 18] = data_buf.get(idx);
      }
      //data_buf.get(file_name_bytes, 0, file_name_length);

      file_name = new String(file_name_bytes, "UTF-8");

      System.out.println("filename: " + file_name);

      // calculate client data
      InetAddress clientHost = request.getAddress();
      int clientPort = request.getPort();

      boolean lastPackage = false;
      boolean CRCcorrect = false;

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
            datasize =  remaining_bytes;

            // this is the last Package to be received. It will have an 4 byte CRC
            datagramm_size += 4;

            lastPackage = true;

         }
         else
            datasize =  DataPackage.PAYLOAD_SIZE;

         datagramm_size += (int)(DATAPACKAGE_HEADER_SIZE + datasize);

         request = new DatagramPacket(new byte[datagramm_size], datagramm_size);
         remaining_bytes -= datasize;

         socket.receive(request);
         actual_fileLegth +=datasize;
         byte[] tmp = request.getData();

         // last parameter is exclusive
         byte[] data = Arrays.copyOfRange(tmp, (int) DATAPACKAGE_HEADER_SIZE, (int) (DATAPACKAGE_HEADER_SIZE + datasize));
         //ByteBuffer tmp_buff = ByteBuffer.wrap(data);
         
         crc.update(data, 0, data.length);


         if (lastPackage){
            byte[] receivedCrc = Arrays.copyOfRange(tmp, (int)(DATAPACKAGE_HEADER_SIZE + datasize), (int)(datagramm_size));
            ByteBuffer tmp_buff = ByteBuffer.wrap(receivedCrc);
            CRCcorrect = (tmp_buff.getInt() == (int) crc.getValue());
            printArray(receivedCrc);   
            System.out.println(CRCcorrect);
         }
         // update the crc with the data array
         
         //if (lastPackage)
            System.out.println("CRC Server: "+((int) crc.getValue()));
         

         
         //System.out.println(new String(Arrays.copyOfRange(tmp, 3, 3 + (int) datasize), "ASCII"));

         byte ack = request.getData()[2];
         // send ACK
         ConfirmationPackage cp = new ConfirmationPackage(ack);
         System.out.println("received bytes: "+ actual_fileLegth);
         System.out.println("data: ");
         printArray(data);
         
         DatagramPacket response = new DatagramPacket(cp.getBytes(), cp.getBytes().length, clientHost, clientPort);
         //cp.send(socket, InetAddress.getByName("localhost"), port);
         socket.send(response);
      }

      //socket.close();
   }
   // helper to print byte arrays
   public static void printArray(byte[] bytes) throws UnsupportedEncodingException
   {
      System.out.println(Arrays.toString(bytes));
   }

   /* 
    * Print ping data to the standard output stream.
    */
   private static void printData(DatagramPacket request) throws Exception
   {
      // Obtain references to the packet's array of bytes.
      byte[] buf = request.getData();

      // Wrap the bytes in a byte array input stream,
      // so that you can read the data as a stream of bytes.
      ByteArrayInputStream bais = new ByteArrayInputStream(buf);

      // Wrap the byte array output stream in an input stream reader,
      // so you can read the data as a stream of characters.
      InputStreamReader isr = new InputStreamReader(bais);

      // Wrap the input stream reader in a bufferred reader,
      // so you can read the character data a line at a time.
      // (A line is a sequence of chars terminated by any combination of \r and \n.) 
      BufferedReader br = new BufferedReader(isr);

      // The message data is contained in a single line, so read this line.
      String line = br.readLine();

      // Print host address and data received from it.
      System.out.println(
         "Received from " + 
         request.getAddress().getHostAddress() + 
         ": " +
         new String(line) );
   }

 
}
