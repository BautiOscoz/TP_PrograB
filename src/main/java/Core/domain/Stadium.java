package Core.domain;

import java.io.Serial;
import java.io.Serializable;

public class Stadium implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private long id;
    private String name;
    private City city;

    public Stadium(long id, String name, City city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
}