import java.io.IOException;

public class Server_1_Run {
	
	public static void main(String []args) throws IOException {
		
		int portNumber_1 = 6789;
		//int portNumber_2 = 6889;
		
		MultiThreadedServer server_1 = new MultiThreadedServer(portNumber_1, true);		
		server_1.up();
		
		
	}

}
