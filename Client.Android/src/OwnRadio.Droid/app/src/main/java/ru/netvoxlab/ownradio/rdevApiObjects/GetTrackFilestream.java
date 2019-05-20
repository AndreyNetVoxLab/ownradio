package ru.netvoxlab.ownradio.rdevApiObjects;

import java.util.HashMap;
import java.util.Map;

public class GetTrackFilestream {
    private String method = "gettrack";
    private String resulttype = "filestream";
    private Map<String, String> fields;

    public GetTrackFilestream(String recid){
        Map<String, String> fields = new HashMap<String, String>();
        fields.put("recid", recid);
        this.fields = fields;
    }
}
