package ru.netvoxlab.ownradio.rdevApiObjects;

import java.util.Map;

public class RegisterUserBody {
    private RegisterUserFields fields;
    private String method = "regnewdevice";

    public RegisterUserBody(String recid, String recname){
        RegisterUserFields userFields = new RegisterUserFields(recid, recname);
        this.fields = userFields;
    }
}
