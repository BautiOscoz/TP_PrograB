package Controller;

import Core.Run.Championship;
import Core.persistance.ChampionshipRepository;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Runs with a desktop session; never accesses PostgreSQL or the user's saved tournament. */
public class BackgroundSimulationTest {
    private static Stage stage;
    private static Parent root;
    private static TournamentController controller;

    public static void main(String[] args) throws Exception {
        Path saveFile = Files.createTempDirectory("background-simulation-").resolve("test.dat");
        BlockingChampionship tournament = new BlockingChampionship();
        Platform.startup(() -> {});
        Platform.setImplicitExit(false);
        try {
            onFx(() -> {
                FXMLLoader loader = new FXMLLoader(BackgroundSimulationTest.class.getResource("/Tournament.fxml"));
                loader.setControllerFactory(type -> new TournamentController(new ChampionshipRepository(saveFile)));
                root = loader.load();
                controller = loader.getController();
                stage = new Stage();
                stage.setScene(new Scene(root, 1080, 720));
                controller.setTournament(tournament, "Background test");
                stage.show();
                invoke("handleSimulateMatchday");
                return null;
            });
            require(tournament.entered.await(10, TimeUnit.SECONDS), "Simulation did not start");
            onFx(() -> {
                require(root.isDisabled(), "Controls must be disabled while simulating");
                require(!tournament.onFxThread, "Simulation must run outside JavaFX");
                invoke("handleSimulateMatchday");
                invoke("handleSimulateKnockoutMatch");
                invoke("handleBack");
                require(stage.getScene().getRoot() == root, "Navigation must be blocked");
                WindowEvent close = new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST);
                Method handler = TournamentController.class.getDeclaredMethod("handleWindowClose", WindowEvent.class);
                handler.setAccessible(true);
                handler.invoke(controller, close);
                require(close.isConsumed(), "Closing during simulation must be blocked");
                require(!Files.exists(saveFile), "Must not save while the tournament is changing");
                return null;
            });
            require(tournament.calls == 1, "Duplicate simulation was launched");
            tournament.release.countDown();
            waitUntilIdle();
            onFx(() -> {
                require(tournament.getPlayedMatches().size() == 8, "Exactly one matchday must be simulated");
                require(!root.lookup("#simulateMatchdayBtn").isDisabled(), "Next matchday button must recover");
                return null;
            });
            require(Files.exists(saveFile), "Completed simulation must be saved");

            AtomicBoolean failureRefreshed = new AtomicBoolean();
            ListChangeListener<Window> closeError = change -> {
                while (change.next()) {
                    for (Window window : change.getAddedSubList()) {
                        if (window != stage) Platform.runLater(window::hide);
                    }
                }
            };
            onFx(() -> {
                Window.getWindows().addListener(closeError);
                Method run = TournamentController.class.getDeclaredMethod("runSimulation",
                        String.class, Callable.class, Consumer.class, Runnable.class);
                run.setAccessible(true);
                run.invoke(controller, "Failure test", (Callable<Void>) () -> {
                    throw new IllegalStateException("Expected test failure");
                }, (Consumer<Void>) value -> { throw new AssertionError("Unexpected success"); },
                        (Runnable) () -> failureRefreshed.set(Platform.isFxApplicationThread()));
                return null;
            });
            waitUntilIdle();
            require(failureRefreshed.get(), "Failure must refresh on the JavaFX thread");
            onFx(() -> {
                Window.getWindows().removeListener(closeError);
                require(!root.lookup("#simulateMatchdayBtn").isDisabled(), "Controls must recover after failure");
                return null;
            });
            System.out.println("Background simulation, duplicate prevention, close protection and failure recovery passed.");
        } finally {
            tournament.release.countDown();
            onFx(() -> { if (stage != null) stage.hide(); return null; });
            Platform.exit();
            Files.deleteIfExists(saveFile);
            Files.deleteIfExists(saveFile.getParent());
        }
    }

    private static void waitUntilIdle() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (onFx(() -> root.isDisabled()) && System.nanoTime() < deadline) Thread.sleep(20);
        require(onFx(() -> !root.isDisabled()), "Operation did not release the interface");
    }

    private static void invoke(String name) throws Exception {
        Method method = TournamentController.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(controller);
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> future = new FutureTask<>(action);
        Platform.runLater(future);
        return future.get(15, TimeUnit.SECONDS);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class BlockingChampionship extends Championship {
        private static final long serialVersionUID = 1L;
        final transient CountDownLatch entered = new CountDownLatch(1);
        final transient CountDownLatch release = new CountDownLatch(1);
        volatile int calls;
        volatile boolean onFxThread;

        BlockingChampionship() throws Exception { super("torneo.json", 2026L); }

        @Override
        public void simulateNextMatchday() {
            calls++;
            onFxThread = Platform.isFxApplicationThread();
            entered.countDown();
            try {
                if (!release.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Test timed out");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            super.simulateNextMatchday();
        }
    }
}
