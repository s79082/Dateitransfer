package rnBeleg;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;  

public class StartPackage extends UdpPackage
{
    byte packageId;
    static String marker = "START";

    long fileLength;
    short fileNameLength;
    byte[] fileName;

    int checksumValue;
    
    public StartPackage(byte pid, byte[] fileName, long fileLength)
    {
        super();
        this.fileName = fileName;
        this.packageId = pid;

        //this.fileNameLength = fileName.length;
        this.fileNameLength = (short) fileName.length;
        //System.out.println(fileName.length);
        this.fileLength = fileLength;
        
        // we need the size to calculate the checksum
        this.size = this.getSize();

        this.checksumValue = this.calculateChecksum();
        
    }

    // returns a byte array representation of this Package
    public byte[] getBytes()
    {

        byte[] tmp = new byte[this.size];

        ByteBuffer buff = ByteBuffer.wrap(tmp);

        // byteorder ala protocoll
        buff.putShort(this.sessionId);
        buff.put(this.packageId);
        buff.put(StartPackage.marker.getBytes());
        buff.putLong(this.fileLength);
        buff.putShort(this.fileNameLength);
        buff.put(this.fileName);
        buff.putInt(this.checksumValue);

        return buff.array();

    }

    // calculates the checksum
    public int calculateChecksum()
    {
        byte[] tmp = new byte[this.getSize() - Integer.BYTES];     // size without checksum
        ByteBuffer buff = ByteBuffer.wrap(tmp);

        // byteorder ala protocoll
        buff.putShort(this.sessionId);
        buff.put(this.packageId);
        buff.put(StartPackage.marker.getBytes());
        buff.putLong(this.fileLength);
        buff.putShort(this.fileNameLength);
        buff.put(this.fileName);

        CRC32 crc = new CRC32();
        crc.update(buff);

        return (int) crc.getValue();
        
    }

    public int getSize()
    {
        return 
        super.getSize()
        + 1 
        + StartPackage.marker.length()  // in UTF-8: 1 char = 1 byte
        + Long.BYTES
        + Short.BYTES
        + this.fileName.length 
        + Integer.BYTES;
    }

}