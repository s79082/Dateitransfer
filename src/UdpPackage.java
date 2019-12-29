//package rnBeleg;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public abstract class UdpPackage
{
    public short sessionId;
    public int size;
    public UdpPackage()
    {
        this.sessionId = 1;
    }

    public abstract byte[] getBytes();
    
    //public abstract void init();
    public int getSize()
    {
        return Short.BYTES;
    }

    public void send(DatagramSocket socket, InetAddress server, int port) throws Exception
    {
        byte[] bytes = this.getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, server, port);
        socket.send(packet); 
    }
}
