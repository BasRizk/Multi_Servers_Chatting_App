import java.io.IOException;

public class Server_2_Run {
	
	public static void main(String []args) throws IOException {
		
		int portNumber_1 = 6789;
		int portNumber_2 = 6889;
		
		MultiThreadedServer server_2 = new MultiThreadedServer(portNumber_2, false);
		server_2.connectWithServer("localhost", portNumber_1);

		server_2.up();
		
	}
	
}
