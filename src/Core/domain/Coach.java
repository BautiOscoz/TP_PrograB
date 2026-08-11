package Core.domain;

import java.time.LocalDate;

public class Coach extends Person {
    private String nationality;
    private int titlesWon;

    public Coach(int id, String name, String lastName, String documentType, LocalDate birthDate,
                 String nationality, int titlesWon) {
        super(id, name, lastName, documentType, birthDate);
        this.nationality = nationality;
        this.titlesWon = titlesWon;
    }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public int getTitlesWon() { return titlesWon; }
    public void setTitlesWon(int titlesWon) { this.titlesWon = titlesWon; }
}