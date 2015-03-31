package com.android.system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class Worker {
    private int HeartBeatDelay = 15000;
    private int localPort = 8000;
    private int Port = 8000;
    private String Host = "";
    private DatagramSocket datagramSocket = null;
    Thread thread = null;

    Runnable mUdpHeartBeat = new Runnable() {
        @Override
        public void run() {
        }
    };
    private boolean startStatus = false;

    public boolean isStart() {
        return startStatus;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }

    public String getHost() {
        return Host;
    }

    public void setHost(String host) {
        Host = host;
    }

    public Worker() {
    }
    public Worker(String host) {
        setHost(host);
    }
    public Worker(String host, int port) {
        setHost(host);
        setPort(port);
    }

    public void start() {
        stop();
        startStatus = true;
        try {
            datagramSocket = new DatagramSocket(getLocalPort());

            //send udp heart beat
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    byte[] sendData = new byte[1024];
                    if (isStart() && datagramSocket != null) {
                        try {
                            datagramSocket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(getHost()), 9876));
                        } catch (UnknownHostException e) {
                        } catch (IOException e) {
                        } catch (Exception e) {
                            timer.cancel();
                        }
                    } else {
                        timer.cancel();
                    }
                }
            }, 0, HeartBeatDelay);

            //receive udp package
            byte[] receiveData = new byte[1024];
            while(isStart()) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    datagramSocket.receive(receivePacket);
                    receiveDatagramPackageHandler(receivePacket);
                } catch (IOException e) {
                }
            }
        } catch (SocketException e) {
        } catch (Exception e) {
        }
    }

    public void stop() {
        if(datagramSocket!= null) {
            datagramSocket.close();
        }
        if(thread != null) {
            try {
                startStatus = false;
                thread.join();
                thread = null;
            } catch (InterruptedException e) {
            }
            datagramSocket = null;
        }
    }

    private void receiveDatagramPackageHandler(DatagramPacket receivePacket) {
        //TODO: handle received udp package
    }
}
