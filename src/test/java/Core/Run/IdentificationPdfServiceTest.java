package Core.Run;

import Core.loader.TournamentLoader;
import Core.report.IdentificationPdfService;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import java.nio.file.Files;

/** Verifies that credentials are readable and repeated avatars are embedded only once. */
public class IdentificationPdfServiceTest {
    public static void main(String[] args) throws Exception {
        var tournament = TournamentLoader.load("torneo.json");
        var folder = Files.createTempDirectory("credentials-test-");
        var output = folder.resolve("participant-ids.pdf");
        var result = new IdentificationPdfService().generate(tournament.getTeams(),
                tournament.getReferees(), "Test tournament", folder, output);
        int expected = tournament.getTeams().stream().mapToInt(t -> t.getPlayers().size() + 1).sum()
                + tournament.getReferees().size();
        if (result.total() != expected || result.missingPhotos().size() != expected)
            throw new AssertionError("Every participant must receive a credential and fallback photo.");
        try (PdfReader reader = new PdfReader(output.toString())) {
            if (reader.getNumberOfPages() < (expected + 2) / 3)
                throw new AssertionError("Missing credential pages.");
            int images = 0;
            for (int i = 1; i < reader.getXrefSize(); i++) {
                var object = reader.getPdfObject(i);
                if (object instanceof PdfDictionary dictionary
                        && PdfName.IMAGE.equals(dictionary.getAsName(PdfName.SUBTYPE))) images++;
            }
            if (images == 0 || images > 8)
                throw new AssertionError("Expected at most four avatars plus alpha masks, found " + images);
            System.out.println("PASS: " + expected + " credentials, " + reader.getNumberOfPages()
                    + " pages, " + images + " embedded images, " + Files.size(output) + " bytes");
            System.out.println(output);
        }
    }
}
