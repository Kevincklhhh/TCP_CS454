import java.net.Socket;

public class client2 {
  public static void main(String[] argv){
    
//    if(argv.length!= 2){
//      System.err.println("usage: client1 <hostname> <hostport>");
//      System.exit(1);
//    }

    try{
      TCPStart.start();
      
      Socket sock = new Socket("100.86.64.158", 12345);

      System.out.println("got socket "+sock);
      
      Thread.sleep(10*1000);

      sock.close();
    }
    catch(Exception e){
      System.err.println("Caught exception:");
      e.printStackTrace();
    }
  }
}
