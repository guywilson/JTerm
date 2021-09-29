package com.guy.jterm;

import com.fazecast.jSerialComm.SerialPort;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Unit test for simple App.
 */
@SuppressWarnings("all")
public final class Main {
    private Main() {

    }

    /**
     * Says hello to the world.
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        SerialPort[] ports = SerialPort.getCommPorts();

        System.out.println("Available ports:");

        for (int i = 0; i < ports.length; i++) {
            System.out.println(
                            "\t" + 
                            i + 
                            ".\t" + 
                            ports[i].getDescriptivePortName() + 
                            ": (" + 
                            ports[i].getSystemPortName() + 
                            ")");
        }

        System.out.println();

        Terminal		terminal;
		LineReader		reader;

		TerminalBuilder builder = TerminalBuilder.builder();

		try {
			terminal = builder.build();

	        reader = 
	        	LineReaderBuilder.builder()
					.terminal(terminal)
					.parser(new DefaultParser())
					.build();
	        
			reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
		}
		catch (Exception e) {
			System.out.println("Failed to create terminal: " + e.getMessage());
			return;
		}

        String portId = reader.readLine("Enter the port number to connect to [0 - " + (ports.length - 1) + "]: ");

        int portNumber = Integer.parseInt(portId);

        if (portNumber < 0 || portNumber >= ports.length) {
            System.out.println("Invalid port number specified");
            return;
        }

        SerialPort port = ports[portNumber];

        port.openPort();
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
        port.setBaudRate(115200);

        InputStream is = null;
        OutputStream os = null;

        try {
            is = port.getInputStream();
            os = port.getOutputStream();
    
            byte[] buffer = new byte[512];
        
            while (true) {
                String data = reader.readLine("[" + port.getSystemPortName() + "]> ") + "\r\n";
    
                try {
                    os.write(data.getBytes());
                }
                catch (IOException ioe) {
                    System.out.println("Failed to write to serial output stream: " + ioe.getMessage());
                    throw new Exception("Failed to write to serial output stream: " + ioe.getMessage());
                }
        
                try {
                    int i = 0;
        
                    while (true) {
                        int ch = is.read();
        
                        if (ch == '\n') {
                            System.out.println(new String(buffer));
                        }
                        else if (ch == -1) {
                            buffer[i] = 0;
                            break;
                        }
        
                        buffer[i] = (byte)ch;
        
                        i++;
                    }
                }
                catch (Exception e) {
                    System.out.println("Error reading from port [" + port.getSystemPortName() + "]: " + e.getMessage());
                    throw e;
                }
            }
        }
        catch (Exception e) {
            System.out.println("\nError, exiting" + e.getMessage());
            return;
        }
        finally {
            port.closePort();
        }
    }
}
