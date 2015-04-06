package com.android.system;

import com.android.system.utils.DataPack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkSessionManager {
    private static final byte[] defaultHeartBeatData = new byte[0];
    private final SessionHandler mSessionHandler;
    private byte[] HeartBeatData = null;
    private int HeartBeatDelay = 2000;
    private int localPort = 0;
    private int Port = 8000;
    private String Host = "";
    private DatagramSocket datagramSocket = null;
    Thread mThread = null;
    Runnable mSessionRunner = new Runnable() {
        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket(getLocalPort());
                final DatagramSocket ds = datagramSocket;

                //send udp heart beat
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if(!isStart()) {
                            timer.cancel();
                            return;
                        }
                        byte[] sendData = getHeartBeatData();
                        try {
                            ds.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(getHost()), getPort()));
                        } catch (UnknownHostException e) {
                        } catch (IOException e) {
                        } catch (Exception e) {
                        } finally {
                            if(!isStart()) {
                                timer.cancel();
                            }
                        }
                    }
                }, 0, HeartBeatDelay);

                //receive udp package
                byte[] receiveData = new byte[1024];
                while(isStart()) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        ds.receive(receivePacket);
                        receiveDatagramPackageHandler(receivePacket);
                    } catch (IOException e) {
                    }
                }
            } catch (SocketException e) {
            } catch (Exception e) {
            }
        }
    };

    private boolean startStatus = false;

    public interface SessionHandler {
        void handleSession(InputStream inputStream, OutputStream outputStream);
    }

    public byte[] getHeartBeatData() {
        if(HeartBeatData == null) {
            return defaultHeartBeatData;
        }
        return HeartBeatData;
    }

    public void setHeartBeatData(byte[] heartBeatData) {
        HeartBeatData = heartBeatData;
    }

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

    public NetworkSessionManager(SessionHandler handler) {
        mSessionHandler = handler;
    }

    public void start() {
        if(isStart()) {
            return;
        }
        startStatus = true;
        mThread = new Thread(mSessionRunner);
        mThread.start();
    }

    public void stop() {
        if(datagramSocket!= null) {
            datagramSocket.close();
            datagramSocket = null;
        }
        if(mThread != null) {
            try {
                startStatus = false;
                mThread.join();
                mThread = null;
            } catch (InterruptedException e) {
            }
        }
    }

    private void receiveDatagramPackageHandler(DatagramPacket receivePacket) {
        String host = receivePacket.getAddress().getHostAddress();
        int port = receivePacket.getPort();
        String uuid = "";
        String msg = "";
        try {
            msg = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            msg = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength() );
        }

        String[] msgs = msg.split(" ");
        uuid = msgs[0];
        if(msgs.length > 1) {
            String[] host_port = msgs[1].split(":");
            host = host_port[0];
            if(host_port.length>1) {
                try {
                    port = Integer.parseInt(host_port[1]);
                } catch (Exception e) {
                }
            }
        }
        createTcpInstance(host,port,uuid);
    }

    private void createTcpInstance(final String host, final int port, final String uuid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket connectSocket = null;
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    //connect host
                    connectSocket = new Socket(host,port);
                    inputStream = connectSocket.getInputStream();
                    outputStream = connectSocket.getOutputStream();

                    //send uuid
                    DataPack.sendDataPack(outputStream, uuid.getBytes("UTF-8"));

                    //handle by SessionHandler
                    if(mSessionHandler != null){
                        mSessionHandler.handleSession(inputStream,outputStream);
                    }
                } catch (IOException e) {
                } finally {
                    //clean
                    if(connectSocket != null) {
                        try {
                            connectSocket.close();
                        } catch (IOException e) {
                        }
                        connectSocket = null;
                    }
                    if(inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                        }
                        inputStream = null;
                    }
                    if(outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                        }
                        outputStream = null;
                    }
                }
            }
        }).start();
    }
}
