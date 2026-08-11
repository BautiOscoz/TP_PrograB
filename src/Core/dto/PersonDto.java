package Core.dto;

import com.google.gson.annotations.SerializedName;

public class PersonDto {
    @SerializedName("tipoDocumento")
    public String documentType;

    @SerializedName("nroDocumento")
    public int documentNumber;

    @SerializedName("nombre")
    public String fullName;

    @SerializedName("fechaNacimiento")
    public String birthDate;
}
