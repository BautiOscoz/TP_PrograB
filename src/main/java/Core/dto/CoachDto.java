package Core.dto;

import com.google.gson.annotations.SerializedName;

public class CoachDto {
    @SerializedName("persona")
    public PersonDto person;

    @SerializedName("pais")
    public CountryDto country;

    @SerializedName("titulosObtenidos")
    public int titlesWon;
}
