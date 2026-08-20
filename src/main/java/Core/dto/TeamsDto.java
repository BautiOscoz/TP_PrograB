package Core.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TeamsDto {
    @SerializedName("equipo")
    public List<TeamDto> team;
}
