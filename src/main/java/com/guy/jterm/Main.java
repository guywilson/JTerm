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
        String      deviceName = null;
        int         baudRate = 9600;
        int         dataBits = 8;
        int         stopBits = SerialPort.ONE_STOP_BIT;
        int         parity = SerialPort.NO_PARITY;
        String      lineEndChars = "\n";

		if (args.length > 0) {
			for (int i = 0;i < args.length;i++) {
                if (args[i].startsWith("-dev")) {
                    deviceName = args[i+1];

                    i++;
                }
                else if (args[i].startsWith("-baud")) {
                    baudRate = Integer.parseInt(args[i+1]);
                    i++;
                }
                else if (args[i].startsWith("-parms")) {
                    String parameters = args[i+1].toUpperCase();
                    
                    dataBits = parameters.charAt(0);

                    switch (parameters.charAt(1)) {
                        case 'N':
                            parity = SerialPort.NO_PARITY;
                            break;

                        case 'O':
                            parity = SerialPort.ODD_PARITY;
                            break;

                        case 'E':
                            parity = SerialPort.EVEN_PARITY;
                            break;

                        default:
                            System.out.println("Invalid parity character");
                            System.exit(-1);
                    }

                    if (parameters.substring(2).equals("1")) {
                        stopBits = SerialPort.ONE_STOP_BIT;
                    }
                    else if (parameters.substring(2).equals("1.5")) {
                        stopBits = SerialPort.ONE_POINT_FIVE_STOP_BITS;
                    }
                    else if (parameters.substring(2).equals("2")) {
                        stopBits = SerialPort.TWO_STOP_BITS;
                    }
                    else {
                        System.out.println("Invalid stop bits");
                        System.exit(-1);
                    }

                    i++;
                }
                else if (args[i].startsWith("-line-end")) {
                    String line_end = args[i+1];

                    if (line_end.equalsIgnoreCase("LF")) {
                        lineEndChars = "\n";
                    }
                    else if (line_end.equalsIgnoreCase("CRLF")) {
                        lineEndChars = "\r\n";
                    }
                }
			}
		}

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

        SerialPort port = null;

        if (deviceName == null) {
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
    
            String portId = reader.readLine("Enter the port number to connect to [0 - " + (ports.length - 1) + "]: ");
    
            int portNumber = Integer.parseInt(portId);
    
            if (portNumber < 0 || portNumber >= ports.length) {
                System.out.println("Invalid port number specified");
                return;
            }
    
            port = ports[portNumber];
        }
        else {
            port = SerialPort.getCommPort(deviceName);
        }

        port.openPort();
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        port.setComPortParameters(baudRate, dataBits, stopBits, parity);

        try {
            PortListener listener = new PortListener(port);

            listener.start();
        
            while (true) {
                listener.getSemaphore().acquire();

                String data = reader.readLine("[" + port.getSystemPortName() + "]> ") + lineEndChars;
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
