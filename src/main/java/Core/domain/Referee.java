package Core.domain;

import java.time.LocalDate;

public class Referee extends Person {
    private String nationality;
    private int refereeYears;

    public Referee(int id, String name, String lastName, String documentType, LocalDate birthDate,
                   String nationality, int refereeYears) {
        super(id, name, lastName, documentType, birthDate);
        this.nationality = nationality;
        this.refereeYears = refereeYears;
    }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public int getRefereeYears() { return refereeYears; }
    public void setRefereeYears(int refereeYears) { this.refereeYears = refereeYears; }
}