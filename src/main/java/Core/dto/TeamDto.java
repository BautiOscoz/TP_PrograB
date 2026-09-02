package Core.dto;

import com.google.gson.annotations.SerializedName;

public class TeamDto {
    @SerializedName("nombre")
    public String name;

    @SerializedName("pais")
    public CountryDto country;

    public int ranking;

    @SerializedName("plantel")
    public SquadDto squad;
}
