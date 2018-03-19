package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import ecdar.abstractions.SimpleComponentsSystemDeclarations;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
import ecdar.mutation.operators.MutationOperator;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.NonRefinementStrategy;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handler for generating test-cases.
 */
class TestCaseGenerationHandler implements ConcurrentJobsHandler {
    private final MutationTestPlan plan;
    private final Consumer<List<MutationTestCase>> testCasesConsumer;

    private final Component testModel;

    private List<MutationTestCase> potentialTestCases;
    private List<MutationTestCase> finishedTestCases;

    private ConcurrentJobsDriver jobsDriver;

    // Progress fields
    private Instant generationStart;
    private String queryFilePath;


    /* Constructors */

    /**
     * Constructs the handler.
     * @param plan the test plan containing options for generation
     * @param testModel the tet model to use
     */
    TestCaseGenerationHandler(final MutationTestPlan plan, final Component testModel, final Consumer<List<MutationTestCase>> testCasesConsumer) {
        this.plan = plan;
        this.testModel = testModel;
        this.testCasesConsumer = testCasesConsumer;
    }


    /* Getters and setters */

    private MutationTestPlan getPlan() {
        return plan;
    }

    private Component getTestModel() {
        return testModel;
    }

    /* Other methods */

    /**
     * Generates mutants and test-cases from them.
     */
    void start() {
        getPlan().setStatus(MutationTestPlan.Status.WORKING);

        testModel.setName(MutationTestPlanController.SPEC_NAME);
        testModel.updateIOList();

        final Instant start = Instant.now();

        // Mutate with selected operators
        potentialTestCases = new ArrayList<>();
        try {
            for (final MutationOperator operator : getPlan().getSelectedMutationOperators())
                potentialTestCases.addAll(operator.generateTestCases(getTestModel()));
        } catch (final MutationTestingException e) {
            handleException(e);
            return;
        }

        potentialTestCases.forEach(testCase -> testCase.getMutant().applyAngelicCompletion());

        getPlan().setMutantsText("Mutants: " + potentialTestCases.size() + " - Execution time: " + MutationTestPlanPresentation.readableFormat(Duration.between(start, Instant.now())));

        // If chosen, apply demonic completion
        if (getPlan().isDemonic()) testModel.applyDemonicCompletion();

        //Rename universal and inconsistent locations
        testModel.getLocations().forEach(location -> {
            if(location.getType().equals(Location.Type.UNIVERSAL)) {
                location.setId("Universal");
            } else if(location.getType().equals(Location.Type.INCONSISTENT)) {
                location.setId("Inconsistent");
            }
        });

        potentialTestCases.forEach(testCase -> testCase.getMutant().getLocations().forEach(location -> {
            if(location.getType().equals(Location.Type.UNIVERSAL)) {
                location.setId("Universal");
            } else if(location.getType().equals(Location.Type.INCONSISTENT)) {
                location.setId("Inconsistent");
            }
        }));

        // Create test cases
        generationStart = Instant.now();
        finishedTestCases = Collections.synchronizedList(new ArrayList<>()); // use synchronized to be thread safe

        try {
            queryFilePath = UPPAALDriver.storeQuery("refinement: " + MutationTestPlanController.MUTANT_NAME + "<=" + MutationTestPlanController.SPEC_NAME, "query");
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        jobsDriver = new ConcurrentJobsDriver(this, potentialTestCases.size());
        jobsDriver.start();
    }


    @Override
    public boolean shouldStop() {
        return getPlan().getStatus().equals(MutationTestPlan.Status.STOPPING) ||
                getPlan().getStatus().equals(MutationTestPlan.Status.ERROR);
    }

    @Override
    public void onStopped() {
        Platform.runLater(() -> getPlan().setStatus(MutationTestPlan.Status.IDLE));
    }
    
    @Override
    public void onAllJobsSuccessfullyDone() {
        try {
            FileUtils.cleanDirectory(new File(UPPAALDriver.getTempDirectoryAbsolutePath()));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        final Text text = new Text("Done");
        text.setFill(Color.GREEN);
        getPlan().writeProgress(text);
        testCasesConsumer.accept(finishedTestCases);
    }
    
    @Override
    public void writeProgress(final int jobsEnded) {
        getPlan().writeProgress("Generating test-cases... (" + jobsEnded + "/" + potentialTestCases.size() + " mutants processed)");
    }

    @Override
    public int getMaxConcurrentJobs() {
        return getPlan().getConcurrentGenerationThreads();
    }

    @Override
    public void startJob(final int index) {
        generateTestCase(potentialTestCases.get(index), getPlan().getVerifytgaTries());
    }

    /**
     * Generates a test-case.
     * @param testCase potential test-case containing the test model, the mutant, and an id
     */
    private void generateTestCase(final MutationTestCase testCase, final int tries) {
        final Component mutant = testCase.getMutant();

        // make a project with the test model and the mutant
        final Project project = new Project();
        mutant.setName(MutationTestPlanController.MUTANT_NAME);
        project.getComponents().addAll(testModel, mutant);
        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
        project.setSystemDeclarations(new SimpleComponentsSystemDeclarations(testModel, mutant));

        new Thread(() -> {
            try {
                // Store the project and the refinement query as backend XML
                final String modelPath;
                try {
                    modelPath = UPPAALDriver.storeBackendModel(project, testCase.getId());
                } catch (IOException | BackendException | URISyntaxException e) {
                    throw new MutationTestingException("Error while storing backend model", e);
                }

                List<String> lines = getVerifytgaInputLines(startVerifytgaProcess(modelPath));

                // If refinement, no test-case to generate.
                // I use endsWith rather than contains,
                // since verifytga sometimes output some weird symbols at the start of this line.
                if (lines.stream().anyMatch(line -> line.endsWith(" -- Property is satisfied."))) {
                    Platform.runLater(this::onGenerationJobDone);
                    return;
                }

                // Verifytga should output that the property is not satisfied
                // If it does not, then this is an error
                if (lines.stream().noneMatch(line -> line.endsWith(" -- Property is NOT satisfied."))) {
                    if (lines.isEmpty()) {
                        if (tries > 1) {
                            final int newTries = tries - 1;
                            Ecdar.showToast("Empty response from verifytga with " + testCase.getId() +
                                    ". We will try again. " + newTries + " tr" + (newTries == 1 ? "y" : "ies") +
                                    " left.");
                            generateTestCase(testCase, tries - 1);
                            return;
                        } else {
                            throw new MutationTestingException("Output from verifytga is empty. Model: " + modelPath);
                        }
                    }

                    throw new MutationTestingException("Output from verifytga not understood: " + String.join("\n", lines) + "\n" +
                            "Model: " + modelPath);
                }

                int strategyIndex = lines.indexOf("Strategy for the attacker:");

                // If no such index, error
                if (strategyIndex < 0) {
                    throw new MutationTestingException("Output from verifytga not understood: " + String.join("\n", lines) + "\n" +
                            "Model: " + modelPath);
                }

                testCase.setStrategy(new NonRefinementStrategy(lines.subList(strategyIndex + 2, lines.size())));

                finishedTestCases.add(testCase);
            } catch (MutationTestingException e) {
                e.printStackTrace();

                // Only show error if the process is not already being stopped
                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                    getPlan().setStatus(MutationTestPlan.Status.ERROR);
                    Platform.runLater(() -> {
                        final String message = "Error while generating test-case " + testCase.getId() + ", " +
                                testCase.getDescription() + ": " + e.getMessage();
                        final Text text = new Text(message);
                        text.setFill(Color.RED);
                        getPlan().writeProgress(text);
                        Ecdar.showToast(message);
                    });
                }

                jobsDriver.onJobDone();
                return;
            }

            // JavaFX elements cannot be updated in another thread, so make it run in a JavaFX thread at some point
            Platform.runLater(this::onGenerationJobDone);
        }).start();
    }

    /**
     * Handles a mutation testing exception.
     * Signal the test plan to stop.
     * Writes an error message with the progress handler.
     * Shows an error message with the toast.
     * @param e the exception to handle
     */
    private void handleException(final MutationTestingException e) {
        e.printStackTrace();

        // Only show error if the process is not already being stopped
        if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
            getPlan().setStatus(MutationTestPlan.Status.ERROR);
            Platform.runLater(() -> {
                final String message = "Error while generating test-cases: " + e.getMessage();
                final Text text = new Text(message);
                text.setFill(Color.RED);
                getPlan().writeProgress(text);
                Ecdar.showToast(message);
            });
        }

        jobsDriver.onJobDone();
    }

    /**
     * Starts varifytga to fetch a strategy.
     * @param modelPath the path to the backend XML project containing the test model and the mutant
     * @return the started process, or null if an error occurs
     */
    private Process startVerifytgaProcess(final String modelPath) throws MutationTestingException {
        final Process process;

        try {
            // Run verifytga to check refinement and to fetch strategy if non-refinement
            final ProcessBuilder builder = new ProcessBuilder(UPPAALDriver.findVerifytgaAbsolutePath(), "-t0", modelPath, queryFilePath);
            process = builder.start();

        } catch (final IOException e) {
            throw new MutationTestingException("Error while starting verifytga", e);
        }

        return process;
    }

    /**
     * Gets the lines from the input stream of verifytga.
     * If an error occurs, this method tells the user and signals this controller to stop.
     * @param process the process running verifytga
     * @return the input lines. Each line is without the newline character.
     * @throws MutationTestingException if an I/O error occurs
     */
    private static List<String> getVerifytgaInputLines(final Process process) throws MutationTestingException {
        List<String> lines;

        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            lines = inputReader.lines().collect(Collectors.toList());
            if (!handlePotentialErrorsFromVerifytga(process)) lines = null;
        } catch (final IOException e) {
            throw new MutationTestingException("I/O exception while reading from verifytga");
        }

        return lines;
    }

    /**
     * Checks for errors in a process.
     * The input stream must be completely read before calling this.
     * Otherwise, we rick getting stuck while reading the error stream.
     * If an error occurs, this method tells the user and signals this controller to stop.
     * @param process process to check for
     * @return true iff process was successful (e.g. no errors was found)
     * @throws IOException if an I/O error occurs
     */
    private static boolean handlePotentialErrorsFromVerifytga(final Process process) throws IOException, MutationTestingException {
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            final List<String> errorLines = errorReader.lines().collect(Collectors.toList());

            if (!errorLines.isEmpty()) {
                throw new MutationTestingException("Error from the error stream of verifytga: " + String.join("\n", errorLines));
            }
        }
        return true;
    }

    /**
     * Is triggered when a test-case generation attempt is done.
     * It updates UI labels to tell user about the progress.
     * Once all test-case generation attempts are done,
     * this method executes the test-cases (not done)
     *
     * This method should be called in a JavaFX thread, since it updates JavaFX elements.
     */
    private synchronized void onGenerationJobDone() {
        getPlan().setTestCasesText("Test-cases: " + finishedTestCases.size() + " - Generation time: " +
                MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now())));

        jobsDriver.onJobDone();
    }

}
