package com.guy.jterm;

import com.fazecast.jSerialComm.SerialPort;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.WebSocket.Listener;
import java.io.IOException;

@SuppressWarnings("all")
public final class Main {
    private static String cmd;

    private Main() {
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("\tjterm <options>");
        System.out.println("\t\t-dev <device-name> (optional, you can choose from a list if not specified)");
        System.out.println("\t\t-baud <baud rate>");
        System.out.println("\t\t-parms <data bits><parity><stop bits> e.g. 8N1, 8E2 etc");
        System.out.println("\t\t-line-end <line feed char(s)> either LF or CRLF");
        System.out.println();
    }

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
                    
                    dataBits = parameters.charAt(0) - 48;

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
                // else {
                //     printUsage();
                //     System.exit(-1);
                // }
			}
		}
        // else {
        //     printUsage();
        //     System.exit(-1);
        // }

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

        CommandHandler handler = new CommandHandler(reader);

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

        char parityChar = 'N';

        if (parity == SerialPort.EVEN_PARITY) {
            parityChar = 'E';
        }
        else if (parity == SerialPort.ODD_PARITY) {
            parityChar = 'O';
        }

        System.out.println(
                    "Opening serial port: " + 
                    port.getSystemPortName() + 
                    " @ " + 
                    baudRate + 
                    " with parameters '" + 
                    dataBits + 
                    parityChar + 
                    stopBits +
                    "'");

        port.openPort();
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        port.setComPortParameters(baudRate, dataBits, stopBits, parity);

        boolean isRunning = true;

        PortListener listener = new PortListener(port);

        listener.start();
    
        try {
            while (isRunning) {
                listener.getSemaphore().acquire();

                if (!handler.isCommandMode()) {
                    String data = reader.readLine("[" + port.getSystemPortName() + "]> ") + lineEndChars;
                    port.writeBytes(data.getBytes(), data.length());
                    
                    listener.getSemaphore().release();
    
                    Thread.sleep(75);
                }
                else {
                    String command = reader.readLine("jTerm: ");

                    if (command.equalsIgnoreCase("quit")) {
                        isRunning = false;
                    }
                    else if (command.equalsIgnoreCase("back")) {
                        handler.setCommandMode(false);
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("\nError, exiting" + e.getMessage());
            e.printStackTrace();
            return;
        }

        listener.stopListener();
        port.closePort();
    }
}
