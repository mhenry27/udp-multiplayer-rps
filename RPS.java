import java.io.*;
import java.util.*;
import java.net.*;

public class RPS {
	public static void main (String[] args) throws Exception {
		
		server s = new server();
		
		System.out.println("hi! please enter your name and either rock, paper, or scissors (seperate them with a comma).");
		
		Scanner scanner = new Scanner (System.in);
		int turn = 1;
		
		
	}//end main
	
	
	static class client {
		public static void main(String[] args) throws Exception {

		    Scanner scanner = new Scanner(System.in);

		    String name = args[0];
		    
		    String sessionId = UUID.randomUUID().toString();

		    String[] serverInfo = args[1].split(":");
		    int port = Integer.parseInt(serverInfo[1]);

		    client c = new client(serverInfo[0], port);

		    // Register with server
		    c.send("JOIN:" + sessionId + ":" + name);

		    int gameId = -1;
		    
		    System.out.println("Waiting for opponent...");

		    // Wait until both players have joined
		    while (true) {

		        String response = c.receiveWithRetry("JOIN:" + sessionId + ":" + name);

		        if (response.startsWith("OPPONENT:")) {

		            String[] opponentInfo = response.split(":", 3);

		            if (opponentInfo.length != 3) {
		                continue;
		            }//end if

		            gameId = Integer.parseInt(opponentInfo[1]);
		            String opponent = opponentInfo[2];

		            System.out.println("Your opponent is " + opponent);
		            System.out.println("Game ID: " + gameId);

		            break;
		        } else if (response.startsWith("WAIT:")) {
		            System.out.println(response.substring(5));

		        } else if (response.startsWith("ERROR:")) {
		            System.out.println(response.substring(6));
		        }
		    }

		    // Ask for player's move
		    System.out.print("Enter your move (rock, paper, scissors): ");
		    String input;

		    while (true) {
		        System.out.print("Enter your move (rock, paper, scissors): ");
		        input = scanner.nextLine().trim().toLowerCase();

		        if (input.equals("rock") ||
		            input.equals("paper") ||
		            input.equals("scissors")) {
		            break;
		        }

		        System.out.println("Invalid move. Please try again.");
		    }


		    // Send move to server
		    c.send("MOVE:" + gameId + ":" + input);

		    System.out.println("Waiting for other player...");

		    // Wait for game result
		    while (true) {

		        String response = c.receiveWithRetry("MOVE:" + gameId + ":"+ input);

		        if (response.startsWith("RESULT:")) {

		            String[] resultInfo = response.split(":", 3);

		            if (resultInfo.length != 3) {
		                continue;
		            }//end if

		            int receivedGameId;

		            try {
		                receivedGameId = Integer.parseInt(resultInfo[1]);
		            } catch (NumberFormatException e) {
		                continue;
		            }//end try catch

		            if (receivedGameId != gameId) {
		                continue;
		            }//end if

		            System.out.println(resultInfo[2]);

		            // Acknowledge the result
		            c.send("ACK:" + gameId);

		            // Wait for server confirmation
		            while (true) {
		                String ackResponse = c.receiveWithRetry("ACK:" + gameId);

		                if (ackResponse.equals("ACK_RECEIVED:" + gameId)) {
		                    System.out.println("Server confirmed ACK.");
		                    break;
		                }//end if
		            }//end while

		            break;
		        } else if (response.startsWith("ERROR:")) {
		            System.out.println(response.substring(6));

		        } else if (response.startsWith("WAIT:")) {
		            System.out.println(response.substring(5));
		        }// end else if
		    }

		    c.ds.close();
		    scanner.close();
		}
			
		DatagramSocket ds;
		InetAddress ip;
		int port;
			
		public client(String hostname, int port) throws Exception {
				 ds = new DatagramSocket ();
				 ds.setSoTimeout(2000);
				 ip = InetAddress.getByName(hostname);
				 this.port = port;
		}//end constructor
		
		public void send(String input) throws Exception {
			byte buf[] = input.getBytes(); 
			
			DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, port);
			ds.send(DpSend);
			
		}//end send	
		
		public String receive() throws Exception {
			byte[] buf = new byte[65535];
			
			DatagramPacket DpReceive = new DatagramPacket(buf, buf.length);

		    ds.receive(DpReceive);

		    String message = new String(
		        DpReceive.getData(),
		        0,
		        DpReceive.getLength()
		    );

		    return message;
		}//end receive
		
		public String receiveWithRetry(String message) throws Exception {
		    while (true) {
		        try {
		            return receive();
		        } catch (SocketTimeoutException e) {
		            // No response received within 2 seconds
		            // Resend the previous message
		            send(message);
		        }//end try catch
		    }//end while
		}//end receive with retry
		
	}//end client
	
	static class server {
		public static void main(String[] args) throws Exception {
			int port = Integer.parseInt(args[0]);
			server s = new server();
			s.run(port);
			
		}
		public void run(int port) throws Exception {
			DatagramSocket ds = new DatagramSocket(port);
			byte[] recieve = new byte[65535];
			
			DatagramPacket DpRecieve = null;
			
			String P1move = null;
			String P2move= null;
			
			String lastResult = null;
			
			int gameId = 1;
			int lastGameId = -1;
			
			boolean P1ack = false;
			boolean P2ack = false;
			
			InetAddress lastP1address = null;
			InetAddress lastP2address = null;

			int lastP1port = 0;
			int lastP2port = 0;
			
			String P1name = null;
			String P2name = null;
			
			String P1session = null;
			String P2session = null;

			Set<String> completedSessions = new HashSet<>();
			
			InetAddress P1address = null;
			InetAddress P2address = null;

			int P1port = 0;
			int P2port = 0;
			
			while (true) {
				DpRecieve = new DatagramPacket(recieve , recieve.length);
				
				ds.receive(DpRecieve);	
				
				InetAddress senderAddress = DpRecieve.getAddress();
				int senderPort = DpRecieve.getPort();
				
				String message = new String(DpRecieve.getData(), 0, DpRecieve.getLength());

				String[] parts = message.split(":", 2);
				
				if (parts.length != 2) {
				    System.out.println("Invalid input: " + message);
				    continue;
				}// end if


				String type = parts[0];
				String value = parts[1].trim();
				
				if (type.equals("JOIN")) {

				    String[] joinInfo = value.split(":", 2);

				    if (joinInfo.length != 2) {
				        continue;
				    }

				    String receivedSession = joinInfo[0];
				    String receivedName = joinInfo[1];

				    // Ignore delayed JOINs from completed games
				    if (completedSessions.contains(receivedSession)) {
				        continue;
				    }

				    if (P1session != null &&
				            P1session.equals(receivedSession)) {
				        // Player 1 is already registered

				    } else if (P2session != null &&
				            P2session.equals(receivedSession)) {
				        // Player 2 is already registered

				    } else if (P1session == null) {

				        P1session = receivedSession;
				        P1name = receivedName;
				        P1address = senderAddress;
				        P1port = senderPort;

				        System.out.println("Player 1 joined: " + P1name);

				    } else if (P2session == null) {

				        P2session = receivedSession;
				        P2name = receivedName;
				        P2address = senderAddress;
				        P2port = senderPort;

				        System.out.println("Player 2 joined: " + P2name);

				    } else {
				        System.out.println("Game is full");
				        continue;
				    }

				    // Send opponent information once both players join
				    if (P1name != null && P2name != null) {

				        sendResponse(ds, "OPPONENT:" + gameId + ":" + P2name,
				                P1address, P1port);

				        sendResponse(ds, "OPPONENT:" + gameId + ":" + P1name,
				                P2address, P2port);
				    }

				} else if (type.equals("MOVE")) {

				    if (P1name == null || P2name == null) {
				        sendResponse(ds, "WAIT:Waiting for opponent",
				                     senderAddress, senderPort);
				        continue;
				    }//end if

				    String[] moveInfo = value.split(":", 2);

				    if (moveInfo.length != 2) {
				        continue;
				    }//end if

				    int receivedGameId;

				    try {
				        receivedGameId = Integer.parseInt(moveInfo[0]);
				    } catch (NumberFormatException e) {
				        continue;
				    }// end try catch

				    if (receivedGameId != gameId) {
				        continue;
				    }//end if

				    String move = moveInfo[1].toLowerCase();

				    if (!move.equals("rock") &&
				        !move.equals("paper") &&
				        !move.equals("scissors")) {

				        sendResponse(ds, "ERROR:Invalid move",
				                     senderAddress, senderPort);
				        continue;
				    }//end if

				    if (P1address.equals(senderAddress)
				            && P1port == senderPort) {

				        if (lastResult != null) {
				        	sendResponse(ds, "RESULT:" + gameId + ":" + lastResult,
				        	        senderAddress, senderPort);
				            continue;
				        }//end if

				        if (P1move == null) {
				            P1move = move;
				        }//end if

				    } else if (P2address.equals(senderAddress)
				            && P2port == senderPort) {

				        if (lastResult != null) {
				        	sendResponse(ds, "RESULT:" + gameId + ":" + lastResult,
				        	        senderAddress, senderPort);
				            continue;
				        }//end if

				        if (P2move == null) {
				            P2move = move;
				        }//end if

				    } else {
				        System.out.println("Unknown client");
				        continue;
				    }//end else if

				} else if (type.equals("ACK")) {

				    int receivedGameId;

				    try {
				        receivedGameId = Integer.parseInt(value);
				    } catch (NumberFormatException e) {
				        continue;
				    }

				    boolean previousPlayer =
				            (lastP1address != null
				                && lastP1address.equals(senderAddress)
				                && lastP1port == senderPort)
				            ||
				            (lastP2address != null
				                && lastP2address.equals(senderAddress)
				                && lastP2port == senderPort);

				    // Handle delayed ACKs from the previous game
				    if (receivedGameId == lastGameId && previousPlayer) {
				        sendResponse(ds, "ACK_RECEIVED:" + lastGameId,
				                senderAddress, senderPort);
				        continue;
				    }

				    // Ignore ACKs for other games
				    if (receivedGameId != gameId || lastResult == null) {
				        continue;
				    }

				    if (P1address != null
				            && P1address.equals(senderAddress)
				            && P1port == senderPort) {

				        P1ack = true;
				        System.out.println("Player 1 acknowledged result");

				        sendResponse(ds, "ACK_RECEIVED:" + gameId,
				                senderAddress, senderPort);

				    } else if (P2address != null
				            && P2address.equals(senderAddress)
				            && P2port == senderPort) {

				        P2ack = true;
				        System.out.println("Player 2 acknowledged result");

				        sendResponse(ds, "ACK_RECEIVED:" + gameId,
				                senderAddress, senderPort);
				    }

				} else {
				    System.out.println("Unknown message type: " + type);
				    continue;
				}//end else 
				
				if (P1move != null && P2move != null && lastResult == null) {

				    lastResult = rps(P1move, P2move);

				    System.out.println(lastResult);

				    sendResponse(ds, "RESULT:" + gameId + ":" + lastResult,
				            P1address, P1port);

				    sendResponse(ds, "RESULT:" + gameId + ":" + lastResult,
				            P2address, P2port);
				}//end if
				
				if (P1ack && P2ack) {
					// Remember previous players for late ACKs
					lastP1address = P1address;
					lastP2address = P2address;

					lastP1port = P1port;
					lastP2port = P2port;

				    System.out.println("Both players acknowledged. Resetting game.");
				    
				    completedSessions.add(P1session);
				    completedSessions.add(P2session);
				    
				    lastGameId = gameId;

				    P1name = null;
				    P2name = null;
				    
				    P1session = null;
				    P2session = null;

				    P1move = null;
				    P2move = null;

				    P1address = null;
				    P2address = null;

				    P1port = 0;
				    P2port = 0;

				    lastResult = null;

				    P1ack = false;
				    P2ack = false;
				    
				    gameId++;

				    System.out.println("Server ready for new players.");
				}//end if
				
				recieve = new byte[65535];
				
			}// end while
		}//end run
		
		public static String rps(String p1, String p2) {

		    if (p1.equals(p2)) {
		        return "It's a tie!";

		    } else if (
		        (p1.equals("rock") && p2.equals("scissors")) ||
		        (p1.equals("paper") && p2.equals("rock")) ||
		        (p1.equals("scissors") && p2.equals("paper"))
		    ) {
		        return "Player one wins!";

		    } else {
		        return "Player two wins!";
		    }
		}//end rps
		
		public void sendResponse(
			    DatagramSocket ds,
			    String message,
			    InetAddress address,
			    int port
			) throws Exception {

			    byte[] buf = message.getBytes();

			    DatagramPacket packet = new DatagramPacket(
			        buf,
			        buf.length,
			        address,
			        port
			    );

			    ds.send(packet);
		}// end send
		
	}//end server
	
	public static StringBuilder data(byte[] a) {
		if (a == null) return null;
		StringBuilder ret = new StringBuilder();
		int i = 0;
		while (a[i] != 0) {
			ret.append((char) a[i]);
			i++;
		}
		
		return ret;
		
	}//end String Builder
	
	

}//end RPS
