package Core.domain;

import java.time.LocalDate;

public class Referee extends Person {
    private Country nationality;
    private int refereeYears;

    public Referee(int id, String name, String lastName, String documentType, int documentNumber, LocalDate birthDate,
                   Country nationality, int refereeYears) {
        super(id, name, lastName, documentType, documentNumber, birthDate);
        this.nationality = nationality;
        this.refereeYears = refereeYears;
    }

    public Country getNationality() { return nationality; }
    public void setNationality(Country nationality) { this.nationality = nationality; }

    public int getRefereeYears() { return refereeYears; }
    public void setRefereeYears(int refereeYears) { this.refereeYears = refereeYears; }
}
