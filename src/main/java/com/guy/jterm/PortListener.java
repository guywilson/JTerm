package com.guy.jterm;

import java.io.InputStream;
import com.fazecast.jSerialComm.SerialPort;

public class PortListener extends Thread
{
    private SerialPort p;

    public PortListener(SerialPort port) {
        this.p = port;
    }

    public void run() {
        InputStream is = null;

        try {
            is = p.getInputStream();
    
            byte[] buffer = new byte[512];

            int i = 0;

            while (true) {
                int ch = is.read();
        
                buffer[i] = (byte)ch;
    
                if (ch == '\n') {
                    System.out.println(new String(buffer, 0, i, "UTF-8"));
                    i = 0;
                }
                else {
                    i++;
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in read thread : " + e.getMessage());
        }
    }
}
