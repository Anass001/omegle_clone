package com.zeneo.omechle.network.stomp;

import android.util.Log;

import com.zeneo.omechle.network.stomp.callback.ListenerSubscription;
import com.zeneo.omechle.network.stomp.callback.ListenerWSNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import ua.naiksoftware.stomp.HeartBeatTask;
import ua.naiksoftware.stomp.dto.StompCommand;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;


public class Stomp {

    private static final String TAG = Stomp.class.getSimpleName();

    public static final int CONNECTED = 1;//Connection completely established
    public static final int NOT_AGAIN_CONNECTED = 2;//Connection process is ongoing
    public static final int DECONNECTED_FROM_OTHER = 3;//Error, no more internet connection, etc.
    public static final int DECONNECTED_FROM_APP = 4;//application explicitely ask for shut down the connection

    private static final String PREFIX_ID_SUBSCIPTION = "sub-";
    private static final String ACCEPT_VERSION_NAME = "accept-version";
    private static final String ACCEPT_VERSION = "1.1,1.0";
    private static final String COMMAND_CONNECT = "CONNECT";
    private static final String COMMAND_CONNECTED = "CONNECTED";
    private static final String COMMAND_MESSAGE = "MESSAGE";
    private static final String COMMAND_RECEIPT = "RECEIPT";
    private static final String COMMAND_ERROR = "ERROR";
    private static final String COMMAND_DISCONNECT = "DISCONNECT";
    private static final String COMMAND_SEND = "SEND";
    private static final String COMMAND_SUBSCRIBE = "SUBSCRIBE";
    private static final String COMMAND_UNSUBSCRIBE = "UNSUBSCRIBE";
    private static final String SUBSCRIPTION_ID = "id";
    private static final String SUBSCRIPTION_DESTINATION = "destination";
    private static final String SUBSCRIPTION_SUBSCRIPTION = "subscription";


    private static final Set<String> VERSIONS = new HashSet<String>();
    static {
        VERSIONS.add("V1.0");
        VERSIONS.add("V1.1");
        VERSIONS.add("V1.2");
    }

    private WebSocket websocket;

    private OkHttpClient client;

    private HeartBeatTask heartBeatTask;

    private int counter;

    private int connection;

    private Map<String, String> headers;

    private int maxWebSocketFrameSize;

    private Map<String, Subscription> subscriptions;

    private ListenerWSNetwork networkListener;

    /**
     * Constructor of a stomp object. Only url used to set up a connection with a server can be instantiate
     *
     * @param url
     *      the url of the server to connect with
     */
    public Stomp(String url, Map<String,String> headersSetup, ListenerWSNetwork stompStates){
        this.counter = 0;

        this.headers = new HashMap<String, String>();
        this.maxWebSocketFrameSize = 16 * 1024;
        this.connection = NOT_AGAIN_CONNECTED;
        this.networkListener = stompStates;
        this.networkListener.onState(NOT_AGAIN_CONNECTED, null);
        this.subscriptions = new HashMap<String, Subscription>();


        client = new OkHttpClient.Builder().pingInterval(10000, TimeUnit.MILLISECONDS).build();
        Request request = new Request.Builder().url(url).build();
        websocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "...Web Socket Openned");
                if(Stomp.this.headers != null){
                    Stomp.this.headers.put(ACCEPT_VERSION_NAME, ACCEPT_VERSION);
                    Stomp.this.headers.put("heart-beat", "10000,10000");
                    transmit(COMMAND_CONNECT, Stomp.this.headers, null);

                    Log.d(TAG, "...Web Socket Openned");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.trim().equals("")) {
                    Log.d(TAG, "<<< PONG");
                    sendHeartBeat();
                    return;
                } else {
                    Log.d(TAG, "<<< " + text);
                }
                Frame frame = Frame.fromString(text);
                boolean isMessageConnected = false;




                if(frame.getCommand().equals(COMMAND_CONNECTED)){
                    Stomp.this.connection = CONNECTED;
                    Stomp.this.networkListener.onState(CONNECTED, frame);

                    Log.d(TAG, "connected to server : " + frame.getHeaders().get("server"));
                    isMessageConnected = true;

                } else if(frame.getCommand().equals(COMMAND_MESSAGE)){
                    String subscription = frame.getHeaders().get(SUBSCRIPTION_SUBSCRIPTION);
                    ListenerSubscription onReceive = Stomp.this.subscriptions.get(subscription).getCallback();

                    if(onReceive != null){
                        onReceive.onMessage(frame.getHeaders(), frame.getBody());
                    } else{
                        Log.e(TAG, "Error : Subscription with id = " + subscription + " had not been subscribed");
                        //ACTION TO DETERMINE TO MANAGE SUBCRIPTION ERROR
                    }

                } else if(frame.getCommand().equals(COMMAND_RECEIPT)){
                    //I DON'T KNOW WHAT A RECEIPT STOMP MESSAGE IS

                } else if(frame.getCommand().equals(COMMAND_ERROR)){
                    Log.e(TAG, "Error : Headers = " + frame.getHeaders() + ", Body = " + frame.getBody());
                    //ACTION TO DETERMINE TO MANAGE ERROR MESSAGE

                } else {

                }

                if(isMessageConnected)
                    Stomp.this.subscribe();
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                Log.d(TAG, "Web Socket disconnecting");
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "Web Socket disconnected");
                if(connection == DECONNECTED_FROM_APP){
                    Log.d(TAG, "Web Socket disconnected");
                    disconnectFromApp();
                } else{
                    Log.w(TAG, "Problem : Web Socket disconnected whereas Stomp disconnect method has never "
                            + "been called.");
                    disconnectFromServer();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                t.printStackTrace();
                Stomp.this.networkListener.onState(400, null);
            }
        });
        client.dispatcher().executorService().shutdown();


    }

    private void sendHeartBeat() {
        Log.d(TAG, "Sending Ping");
        websocket.send("\r\n");
    }

    /**
     * Send a message to server thanks to websocket
     *
     * @param command
     *      one of a frame property, see {@link Frame} for more details
     * @param headers
     *      one of a frame property, see {@link Frame} for more details
     * @param body
     *      one of a frame property, see {@link Frame} for more details
     */
    private void transmit(String command, Map<String, String> headers, String body){
        String out = Frame.marshall(command, headers, body);
        Log.d(TAG, ">>> " + out);
        while (true) {
            if (out.length() > this.maxWebSocketFrameSize) {
                List<StompHeader> stompHeaders = new ArrayList<>();
                for (String key: headers.keySet()) {
                    stompHeaders.add(new StompHeader(key, headers.get(key)));
                }
                new StompMessage(StompCommand.CONNECT, stompHeaders, null).compile(false);
                out = out.substring(this.maxWebSocketFrameSize);
            } else {
                this.websocket.send(out);
                break;
            }
        }
    }

    /**
     * disconnection come from the server, without any intervention of client side. Operations order is very important
     */
    private void disconnectFromServer(){
        if(this.connection == CONNECTED){
            this.connection = DECONNECTED_FROM_OTHER;
            this.websocket.close(1000, null);
            this.networkListener.onState(this.connection, null);
        }
    }

    /**
     * disconnection come from the app, because the public method disconnect was called
     */
    private void disconnectFromApp(){
        if(this.connection == DECONNECTED_FROM_APP){
            this.websocket.close(1000, null);
            this.networkListener.onState(this.connection, null);
        }
    }

    /**
     * Close the web socket connection with the server. Operations order is very important
     */
    public void disconnect(){
        if(this.connection == CONNECTED){
            this.connection = DECONNECTED_FROM_APP;
            transmit(COMMAND_DISCONNECT, null, null);
        }
    }

    /**
     * Send a simple message to the server thanks to the body parameter
     *
     *
     * @param destination
     *      The destination through a Stomp message will be send to the server
     * @param headers
     *      headers of the message
     * @param body
     *      body of a message
     */
    public void send(String destination, Map<String,String> headers, String body){
        if(this.connection == CONNECTED){
            if(headers == null)
                headers = new HashMap<String, String>();

            if(body == null)
                body = "";

            headers.put(SUBSCRIPTION_DESTINATION, destination);

            transmit(COMMAND_SEND, headers, body);
        }
    }

    /**
     * Allow a client to send a subscription message to the server independently of the initialization of the web socket.
     * If connection have not been already done, just save the subscription
     *
     * @param subscription
     *      a subscription object
     */
    public void subscribe(Subscription subscription){
        subscription.setId(PREFIX_ID_SUBSCIPTION + this.counter++);
        this.subscriptions.put(subscription.getId(), subscription);

        if(this.connection == CONNECTED){
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(SUBSCRIPTION_ID, subscription.getId());
            headers.put(SUBSCRIPTION_DESTINATION, subscription.getDestination());

            subscribe(headers);
        }
    }

    /**
     * Subscribe to a Stomp channel, through messages will be send and received. A message send from a determine channel
     * can not be receive in an another.
     *
     */
    private void subscribe(){
        if(this.connection == CONNECTED){
            for(Subscription subscription : this.subscriptions.values()){
                Map<String, String> headers = new HashMap<>();
                headers.put(SUBSCRIPTION_ID, subscription.getId());
                headers.put(SUBSCRIPTION_DESTINATION, subscription.getDestination());

                subscribe(headers);
            }
        }
    }

    /**
     * Send the subscribe to the server with an header
     * @param headers
     *      header of a subscribe STOMP message
     */
    private void subscribe(Map<String, String> headers){
        transmit(COMMAND_SUBSCRIBE, headers, null);
    }

    /**
     * Destroy a subscription with its id
     *
     * @param id
     *      the id of the subscription. This id is automatically setting up in the subscribe method
     */
    public void unsubscribe(String id){
        if(this.connection == CONNECTED){
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(SUBSCRIPTION_ID, id);

            this.subscriptions.remove(id);
            this.transmit(COMMAND_UNSUBSCRIBE, headers, null);
        }
    }

}
