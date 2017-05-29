package com.azeesoft.rccardriver.tools.wifi;

/**
 * Created by azizt on 5/20/2017.
 */

public class CommConstants {

    /*
    * Common data sent from both Server and Client
    * */

    public enum RESPONSE_DATA_FLAGS_SUCCESS {AZEE_IP_HANDSHAKE}
    public enum RESPONSE_DATA_FLAGS_FAILURE {MAX_CONN_REACHED, NON_JSON_DATA}

    public final static String NAME_SUCCESS = "success";
    public final static String NAME_SERVER_REQUEST_ID = "server_request_id";
    public final static String NAME_CLIENT_REQUEST_ID = "client_request_id";
    public final static String NAME_FLAGS_ARRAY = "flags_array";
    public final static String NAME_NON_JSON_DATA = "non_json_data";
    public final static String NAME_MESSAGE = "message";

    public final static String NAME_SPEECH_DATA = "speech_data";



    /*
    * Data sent from Server
    * */



    /*
    * Data sent from Client
    * */

    public final static String REQUEST_NAME_ARDUINO_BLUETOOTH_DATA = "arduino_bluetooth_data";
    public final static String REQUEST_NAME_CONNECT_TO_RC_CAR = "connect_to_rc_car";
    public final static String REQUEST_NAME_DISCONNECT_FROM_RC_CAR = "disconnect_from_rc_car";
    public final static String REQUEST_NAME_RESET_WIFI_CONNECTIONS = "reset_wifi_connections";
    public final static String REQUEST_NAME_START_WIFI_SERVER = "start_wifi_server";
    public final static String REQUEST_NAME_SPEAK = "speak";

//    NEED RESPONSE
//    =============
    public final static String REQUEST_NAME_START_HLS_SERVER = "start_rtsp_server";
    public final static String REQUEST_NAME_STOP_HLS_SERVER = "stop_rtsp_server";

}
