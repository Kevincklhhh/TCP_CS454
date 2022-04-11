import java.net.Socket;
import java.net.ServerSocket;

public class server2 {
  public static void main(String[] argv){
    
    if(argv.length!= 1){
      System.err.println("usage: server1 <hostport>");
      System.exit(1);
    }

    try{
      TCPStart.start();
      
      ServerSocket sock = new ServerSocket(Integer.parseInt(argv[0]));
      Socket connSock = sock.accept();

      System.out.println("got socket "+connSock);

      Thread.sleep(10*1000);
      connSock.close();
      sock.close();
    }
    catch(Exception e){
      System.err.println("Caught exception "+e);
    }
  }
}
