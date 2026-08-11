package Core.dto;

import com.google.gson.annotations.SerializedName;

public class SquadDto {
    @SerializedName("jugadores")
    public PlayersDto players;

    @SerializedName("dt")
    public CoachDto coach;
}
