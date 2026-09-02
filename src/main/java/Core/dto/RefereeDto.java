package Core.dto;

import com.google.gson.annotations.SerializedName;

public class RefereeDto {
    @SerializedName("persona")
    public PersonDto person;

    @SerializedName("pais")
    public CountryDto country;

    @SerializedName("aniosReferato")
    public int refereeYears;
}
