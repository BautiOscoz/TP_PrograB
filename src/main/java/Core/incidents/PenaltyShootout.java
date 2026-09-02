package Core.incidents;

import java.io.Serial;
import java.io.Serializable;

public class PenaltyShootout extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public PenaltyShootout (int minuto){
        super (minuto);
    }
}
