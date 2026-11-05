package Controller;

import Core.Run.Championship;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

import Core.report.IdentificationPdfService;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;

import java.util.List;
import java.util.function.Consumer;


import GUI.DialogService;

/** Coordinates file selection and background PDF generation. */
public final class IdentificationExportController {
    private final AnchorPane tournamentPane;
    private final TextArea reportArea;
    private final Runnable showReport;
    private final DialogService dialogs;
    private final Consumer<Boolean> setBusy;

    public IdentificationExportController(AnchorPane tournamentPane,
            TextArea reportArea,
            Runnable showReport,
            DialogService dialogs, Consumer<Boolean> setBusy) {
        this.tournamentPane = tournamentPane;
        this.reportArea = reportArea;
        this.showReport = showReport;
        this.dialogs = dialogs;
        this.setBusy = setBusy;
    }
    public void export(Championship championship, String tournamentName) {

        if (championship == null) {
            dialogs.showError("Load a tournament first.");
            return;
        }

        // Select the photographs folder.
        dialogs.showInformation("Participant photographs",
                "Name each photograph with the person's document number, for example 12345678.jpg. "
                        + "JPG, JPEG and PNG are supported. Missing photos use a sample illustration and are reported in the result.");
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle(
                "Choose the photographs folder"
        );

        File photos = directoryChooser.showDialog(
                tournamentPane.getScene().getWindow()
        );

        if (photos == null) {
            return;
        }

        // Select where to save the PDF.
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Save participant IDs");
        chooser.setInitialFileName("participant-ids.pdf");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        File selected = chooser.showSaveDialog(
                tournamentPane.getScene().getWindow()
        );

        if (selected == null) {
            return;
        }

        if (!selected.getName()
                .toLowerCase(java.util.Locale.ROOT)
                .endsWith(".pdf")) {

            dialogs.showError("The filename must end in .pdf.");
            return;
        }

        var teams = List.copyOf(championship.getTeams());
        var referees = List.copyOf(championship.getReferees());
        String title = tournamentName;

        // Generate in the background without blocking JavaFX.
        Task<IdentificationPdfService.Result> task = new Task<>() {

            @Override
            protected IdentificationPdfService.Result call()
                    throws Exception {

                return new IdentificationPdfService().generate(
                        teams,
                        referees,
                        title,
                        photos.toPath(),
                        selected.toPath()
                );
            }
        };

        setBusy.accept(true);

        task.setOnSucceeded(event -> {

            setBusy.accept(false);

            var result = task.getValue();

            showReport.run();

            String message =
                    "PDF generated: " + selected.getAbsolutePath()
                            + "\nParticipant IDs: " + result.total()
                            + "\nMissing photographs: "
                            + result.missingPhotos().size();

            reportArea.setText(
                    message + "\n\n"
                            + String.join("\n", result.missingPhotos())
            );

            dialogs.showInformation("Participant IDs", message);
        });

        task.setOnFailed(event -> {

            setBusy.accept(false);

            dialogs.showError(
                    "Could not generate the PDF: "
                            + task.getException().getMessage()
            );
        });

        new Thread(task, "identificaciones-pdf").start();
    }
}
