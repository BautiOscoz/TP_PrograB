package Core.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlayersDto {
    @SerializedName("jugador")
    public List<PlayerDto> player;
}
