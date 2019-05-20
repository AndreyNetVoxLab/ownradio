package ru.netvoxlab.ownradio.rdevApiObjects;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Класс для сериализации данных для передачи в хранимую процедуру (rdev)
 */
public class ExecuteProcedureObject {
    /**
     * Название хранимки
     */
    @SerializedName("name")
    String name;

    /**
     * Параметры хранимки
     */
    @SerializedName("parameters")
    ArrayList<StoredProcParameter> parameters = new ArrayList<>();


    public ExecuteProcedureObject(String name, ArrayList<StoredProcParameter> parameters){
        this.name = name;
        this.parameters = parameters;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(ArrayList<StoredProcParameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(StoredProcParameter parameter){
        parameters.add(parameter);
    }
}
