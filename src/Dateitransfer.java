
public class Dateitransfer
{
    public static void main(String[] args)
    {
        String server;
        int port = 1024;
        if (args[0].equals("idefix"))
        {
            server = "idefix.informatik.htw-dresden.de";
            port = 3333;
        }
        else
            server = args[0];

        Thread client_thread = new Thread() {
            public void run() {

                ClientUdp client = new ClientUdp(); 

                try{
                    //client.main(server, args[1], 3333);
                }
                catch(Exception e)
                {
                    System.exit(-1);
                }
            }
        };
        

        Thread server_thread = new Thread() {
            public void run() {
                ServerUdp server = new ServerUdp();
                try{
                    server.main(args);
                }
                catch(Exception e)
                {
                    System.exit(-1);
                }
            }

        };

        server_thread.start();  
        client_thread.start();

try{
        server_thread.join();
        client_thread.join();   
}catch(Exception e)
{
    System.exit(-1);
}   
    }

}