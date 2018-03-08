import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Users_OnServer_2 {
	
	public static void main(String [] args) throws UnknownHostException, IOException {
		
		AppUser user = new AppUser(6889, null);
		user.runApp();
	
	}
}
