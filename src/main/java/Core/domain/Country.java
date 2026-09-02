package Core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class Country implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String name;

    public Country(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A country must have a name.");
        }
        this.name = name.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Country country)) return false;
        return name.equalsIgnoreCase(country.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return name;
    }
}
