package Core.domain;

import java.time.LocalDate;

public class Coach extends Person {
    private Country nationality;
    private int titlesWon;

    public Coach(int id, String name, String lastName, String documentType, int documentNumber, LocalDate birthDate,
                 Country nationality, int titlesWon) {
        super(id, name, lastName, documentType, documentNumber, birthDate);
        this.nationality = nationality;
        this.titlesWon = titlesWon;
    }

    public Country getNationality() { return nationality; }
    public void setNationality(Country nationality) { this.nationality = nationality; }

    public int getTitlesWon() { return titlesWon; }
    public void setTitlesWon(int titlesWon) { this.titlesWon = titlesWon; }
}
