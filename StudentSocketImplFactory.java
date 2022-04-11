import java.net.*;

//---------------------------------------------------
//
// class StudentSocketImplFactory
//
// this object is what actually creates each INSTANCE of a
// SocketImpl object.  In TCPStart.main(), we call
//
//     Socket.setSocketImplFactory( new StudentSocketImplFactory(D) );
//
// (this is a static function)
// so, when we create a java Socket, it will make a call to
// createSocketImpl(), and the Socket will use OUR code!!!
//
//---------------------------------------------------
class StudentSocketImplFactory implements SocketImplFactory {

    // the Demultiplexer has to be known to every SocketImpl, so that it
    // can communicate with it
    private Demultiplexer D;


    public StudentSocketImplFactory(Demultiplexer D) {
        super();
        this.D = D;
    }

    // Socket object makes this call to get one instance of SocketImpl.
    // reminder: each socket will get a DIFFERENT instance of
    // SocketImpl. this is GOOD, so that we will have one TCPConnection
    // for each Socket!!
    public SocketImpl createSocketImpl() {
        return ( new StudentSocketImpl(D) );
    }
}
