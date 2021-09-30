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
    public static void main(String[] args)
    {
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
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        port.setBaudRate(115200);

        try {
            PortListener listener = new PortListener(port);

            listener.start();
        
            while (true) {
                listener.getSemaphore().acquire();

                String data = reader.readLine("[" + port.getSystemPortName() + "]> ") + "\r\n";
                port.writeBytes(data.getBytes(), data.length());
                
                listener.getSemaphore().release();

                Thread.sleep(75);
            }
        }
        catch (Exception e) {
            System.out.println("\nError, exiting" + e.getMessage());
            e.printStackTrace();
            return;
        }
    }
}
