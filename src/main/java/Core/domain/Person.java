package Core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public abstract class Person implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String lastName;
    private String documentType;
    private int documentNumber;
    private LocalDate birthDate;

    public Person(int id, String name, String lastName, String documentType,
                  int documentNumber, LocalDate birthDate) {
        if (name == null || name.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("A person must have a first name and a last name.");
        }
        if (documentType == null || documentType.isBlank() || documentNumber <= 0) {
            throw new IllegalArgumentException("A person must have a valid document.");
        }
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A person must have a valid birth date.");
        }
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public int getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(int documentNumber) { this.documentNumber = documentNumber; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}
