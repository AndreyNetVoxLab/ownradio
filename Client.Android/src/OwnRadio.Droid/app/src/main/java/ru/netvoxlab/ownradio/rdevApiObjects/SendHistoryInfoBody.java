package ru.netvoxlab.ownradio.rdevApiObjects;

import java.util.HashMap;
import java.util.Map;

public class SendHistoryInfoBody {
    String method = "savehistory";
    Map<String, String> fields;

    public SendHistoryInfoBody(String trackid, String deviceid, String recid, String islisten, String lastlisten, String userid){
        Map<String, String> fields = new HashMap<String, String>();
        fields.put("trackid", trackid);
        fields.put("deviceid", deviceid);
        fields.put("recid", recid);
        fields.put("islisten", islisten);
        fields.put("lastlisten", lastlisten);
        fields.put("userid", userid);
        this.fields = fields;
    }
}