package Core.report;

import Core.domain.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Generates printable participant credentials without depending on JavaFX. */
public class IdentificationPdfService {
    public record Result(int total, List<String> missingPhotos) {
        public Result { missingPhotos = List.copyOf(missingPhotos); }
    }

    private record Participant(Person person, String role, String details) {}

    /** Photos must be named with the document number, followed by .jpg, .jpeg or .png. */
    public Result generate(List<Team> teams, List<Referee> referees, String title,
                           Path photos, Path destination) throws IOException {
        if (!Files.isDirectory(photos)) {
            throw new IllegalArgumentException("Select an existing photographs folder.");
        }
        List<Participant> participants = new ArrayList<>();
        for (Team team : teams) {
            for (Player player : team.getPlayers()) {
                participants.add(new Participant(player, "PLAYER", "Team: " + team.getName()
                        + "\nPosition: " + player.getPosition() + " | Shirt: " + player.getShirtNumber()));
            }
            Coach coach = team.getCoach();
            participants.add(new Participant(coach, "COACH", "Team: " + team.getName()
                    + "\nNationality: " + coach.getNationality().getName()
                    + " | Titles: " + coach.getTitlesWon()));
        }
        for (Referee referee : referees) {
            participants.add(new Participant(referee, "REFEREE", "Nationality: "
                    + referee.getNationality().getName() + "\nExperience: "
                    + referee.getRefereeYears() + " years"));
        }
        if (participants.isEmpty()) throw new IllegalArgumentException("No participants to export.");
        Path output = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(output.getParent(), "credentials-", ".pdf");
        List<String> missing = new ArrayList<>();
        try {
            try (OutputStream stream = Files.newOutputStream(temporary)) {
                Document document = new Document(PageSize.A4, 36, 36, 36, 36);
                PdfWriter writer = PdfWriter.getInstance(document, stream);
                document.addTitle(title == null ? "Participant credentials" : title);
                document.open();
                try {
                    for (int i = 0; i < participants.size(); i++) {
                        if (i > 0 && i % 3 == 0) document.newPage();
                        Participant participant = participants.get(i);
                        PdfPTable card = new PdfPTable(new float[]{1, 3});
                        card.setWidthPercentage(100);
                        card.setKeepTogether(true);
                        card.setSpacingAfter(16);
                        PdfPCell heading = cell((title == null || title.isBlank() ? "Tournament" : title)
                                + " - " + participant.role(), 12, Font.BOLD);
                        heading.setColspan(2);
                        heading.setBackgroundColor(new java.awt.Color(225, 235, 245));
                        card.addCell(heading);
                        PdfPCell photoCell = cell("PHOTO\nNOT AVAILABLE", 10, Font.NORMAL);
                        Image photo = findPhoto(photos, participant.person(), missing);
                        if (photo != null) {
                            photo.scaleToFit(90, 105);
                            photoCell = new PdfPCell(photo, false);
                            photoCell.setPadding(8);
                        }
                        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        card.addCell(photoCell);
                        Person person = participant.person();
                        card.addCell(cell(person.getName() + " " + person.getLastName()
                                + "\nID: " + person.getId()
                                + "\nDocument: " + person.getDocumentType() + " " + person.getDocumentNumber()
                                + "\nBirth date: " + person.getBirthDate()
                                + "\nRole: " + participant.role() + "\n" + participant.details(), 11, Font.NORMAL));
                        Barcode128 barcode = new Barcode128();
                        barcode.setCodeType(Barcode.CODE128);
                        barcode.setCode(participant.role() + "-" + person.getDocumentNumber());
                        barcode.setBarHeight(28);
                        Image barcodeImage = barcode.createImageWithBarcode(writer.getDirectContent(), null, null);
                        PdfPCell barcodeCell = new PdfPCell(barcodeImage, false);
                        barcodeCell.setColspan(2);
                        barcodeCell.setPadding(8);
                        barcodeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        card.addCell(barcodeCell);
                        document.add(card);
                    }
                } finally {
                    document.close();
                }
            }
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            return new Result(participants.size(), missing);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private PdfPCell cell(String text, int size, int style) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, size, style)));
        cell.setPadding(8);
        return cell;
    }

    private Image findPhoto(Path folder, Person person, List<String> missing) {
        for (String extension : List.of(".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG")) {
            Path file = folder.resolve(person.getDocumentNumber() + extension);
            if (!Files.isRegularFile(file)) continue;
            try {
                return Image.getInstance(file.toAbsolutePath().toString());
            } catch (IOException | RuntimeException exception) {
                // A damaged photo must not prevent exporting the other participants.
            }
        }
        missing.add(person.getName() + " " + person.getLastName()
                + " - missing or unreadable photo: " + person.getDocumentNumber() + ".jpg/.png");
        return null;
    }
}
