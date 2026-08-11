package Core.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class PlayerDto {
    @SerializedName("posicion")
    public String position;

    @SerializedName("persona")
    public PersonDto person;

    @SerializedName("caracteristicas")
    public Map<String, Integer> characteristics;

    @SerializedName("estadisticas")
    public Map<String, Integer> statistics;
}
