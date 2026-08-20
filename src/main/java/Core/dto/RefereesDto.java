package Core.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RefereesDto {
    @SerializedName("arbitro")
    public List<RefereeDto> referee;
}
