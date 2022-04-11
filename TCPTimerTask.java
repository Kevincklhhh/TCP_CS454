
import java.util.TimerTask;
import java.util.Timer;

public class TCPTimerTask extends TimerTask {

  private BaseSocketImpl sock;
  private Object ref;

  /**
   * register timer event for TCP statck
   * @param tcpTtimer TImer object to use
   * @param delay length of time before timer in milliseconds
   * @param sock socket implementation to call sock.handleTimer(ref)
   * @param ref generic object of information to pass back
   */
  public TCPTimerTask(Timer tcpTimer, long delay, BaseSocketImpl sock, Object ref){
    this.sock = sock;
    this.ref = ref;
    tcpTimer.schedule(this, delay);
  }

  public void run(){
    sock.handleTimer(ref);
  }
}

