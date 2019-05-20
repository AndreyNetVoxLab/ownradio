package ru.netvoxlab.ownradio.rdevApiObjects;

import java.util.HashMap;
import java.util.Map;

public class DeviceInfoBody {
    Map<String, String> fields;
    String method = "showdeviceinfo";

    public DeviceInfoBody(String deviceid){
        Map<String, String> fields = new HashMap<String, String>();
        fields.put("recid", deviceid);
        this.fields = fields;
    }

}
