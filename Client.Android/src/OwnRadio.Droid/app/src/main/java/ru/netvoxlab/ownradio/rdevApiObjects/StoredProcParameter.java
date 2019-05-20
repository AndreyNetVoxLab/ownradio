package ru.netvoxlab.ownradio.rdevApiObjects;

import com.google.gson.annotations.SerializedName;

/**
 * Класс для хранения параметров хранимой процедуры
 */
public class StoredProcParameter {
    @SerializedName("name")
    String name;

    @SerializedName("value")
    String value;

    @SerializedName("type")
    String type;

    /**
     * Конструктор класса StoredProcParameter
     * @param name      Название параметра
     * @param value     Значение параметра
     * @param type      rdev тип параметра
     */
    public StoredProcParameter(String name, String value, String type){
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }
}
