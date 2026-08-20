package Core.dto;

import com.google.gson.annotations.SerializedName;

public class TournamentDto {
    @SerializedName("equipos")
    public TeamsDto teams;

    @SerializedName("arbitros")
    public RefereesDto referees;
}
