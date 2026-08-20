package Core.dto;

import com.google.gson.annotations.SerializedName;

public class TeamDto {
    @SerializedName("nombre")
    public String name;

    @SerializedName("pais")
    public String country;

    public int ranking;

    @SerializedName("plantel")
    public SquadDto squad;
}
