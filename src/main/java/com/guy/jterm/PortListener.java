package com.guy.jterm;

import java.util.concurrent.Semaphore;
import com.fazecast.jSerialComm.SerialPort;

public class PortListener extends Thread
{
    private SerialPort p;
    private Semaphore s;

    private final static int STATE_INIT =               0;
    private final static int STATE_ACQUIRE =            1;
    private final static int STATE_READ =               2;
    private final static int STATE_PRINTLINE =          3;
    private final static int STATE_RELEASE =            4;

    private final static int ZERO_BYTE_RETRY_COUNT =    5;

    public PortListener(SerialPort port) {
        this.p = port;

        this.s = new Semaphore(1, true);
    }

    public Semaphore getSemaphore() {
        return s;
    }

    public void run() {
        try {
            byte[] buffer = new byte[128];
            byte[] bytesToRead = new byte[2];

            int i = 0;
            int state = STATE_INIT;
            int zeroBytesReadCount = 0;

            while (true) {
                switch (state) {
                    case STATE_INIT:
                        Thread.sleep(500);
                        state = STATE_ACQUIRE;
                        break;

                    case STATE_ACQUIRE:
                        getSemaphore().acquire();
                        Thread.sleep(75);
                        state = STATE_READ;
                        break;

                    case STATE_READ:
                        int bytesRead = this.p.readBytes(bytesToRead, 1);

                        if (bytesRead > 0) {
                            buffer[i] = bytesToRead[0];

                            zeroBytesReadCount = 0;

    //                      System.out.print(String.format("%02X", buffer[i]));
            
                            if (buffer[i] == '\n') {
                                state = STATE_PRINTLINE;
                            }
                            else {
                                i++;
                            }
                        }
                        else if (bytesRead == 0) {
                            zeroBytesReadCount++;
                            Thread.sleep(100);

                            if (zeroBytesReadCount == ZERO_BYTE_RETRY_COUNT) {
                                state = STATE_RELEASE;
                                zeroBytesReadCount = 0;
                            }
                        }
                        else {
                            throw new Exception("Error reading from serial port");
                        }
                        break;

                    case STATE_PRINTLINE:
//                      System.out.println();
                        System.out.println(new String(buffer, 0, i, "UTF-8"));
                        i = 0;

                        state = STATE_READ;
                        break;

                    case STATE_RELEASE:
                        getSemaphore().release();
                        state = STATE_ACQUIRE;
                        Thread.sleep(100);
                        break;
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in read thread : " + e.getMessage());
        }
    }
}
