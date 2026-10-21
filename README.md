# UDP Multiplayer Rock-Paper-Scissors

## Project Overview
A multiplayer Rock-Paper-Scissors game developed in Java using UDP socket programming. The project demonstrates client-server communication, reliable message delivery over an unreliable transport protocol, and handling of packet loss.

## Technologies Used
- Java
- UDP Socket Programming
- Client-Server Architecture
- Acknowledgments (ACKs) and Retransmissions

## Key Features
- Multiplayer Rock-Paper-Scissors gameplay
- UDP-based communication between clients and server
- ACK-based reliability mechanisms
- Retransmission handling for lost packets
- Support for testing under simulated network loss
- Client and server functionality implemented in a single Java source file

## Compilation and Execution

The `RPS.java` file contains nested server and client classes.

Compile the program:

```bash
javac RPS.java
```

Open three separate terminals.

**Terminal 1 — Server**
```bash
java 'RPS$server' 9000
```

**Terminal 2 — Client 1**
```bash
java 'RPS$client' Mimi localhost:9000
```

**Terminal 3 — Client 2**
```bash
java 'RPS$client' Sam localhost:9000
```

## Testing Under Packet Loss

To simulate unreliable network conditions, start the server as described above and run two lossy proxies:

```bash
python3 lossy_proxy.py 9001 localhost 9000 20 20 1000
python3 lossy_proxy.py 9002 localhost 9000 20 20 1000
```

Run the clients against ports `9001` and `9002` instead of `9000`.

*Note: The lossy proxy script is required separately for this testing procedure.*

## Technical Concepts Demonstrated
- Network protocol design
- Reliable communication over UDP
- Timeout and retransmission mechanisms
- Packet-loss simulation
- Multiplayer client-server coordination

## Documentation
See the accompanying project write-up for additional implementation details, reliability explanations, and testing discussions.