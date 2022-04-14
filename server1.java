import java.net.Socket;
import java.net.ServerSocket;

public class server1 {
  public static void main(String[] argv){
    
//    if(argv.length!= 1){
//      System.err.println("usage: server1 <hostport>");
//      System.exit(1);
//    }

    try{
      TCPStart.start();
      //Integer.parseInt(argv[0])
      ServerSocket sock = new ServerSocket(12345);
      Socket connSock = sock.accept();

      System.out.println("got socket "+connSock);

      Thread.sleep(10*1000);
    }
    catch(Exception e){
      System.err.println("Caught exception "+e);
    }
  }
}
