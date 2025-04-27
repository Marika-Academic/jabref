package org.jabref.gui.newentryunified;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryUnifiedViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryUnifiedViewModel.class);

    private final GuiPreferences preferences;
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UiTaskExecutor taskExecutor;
    private final FileUpdateMonitor fileUpdateMonitor;

    private final BooleanProperty executing;
    private final BooleanProperty executedSuccessfully;

    private final StringProperty idText;
    private final Validator idTextValidator;
    private final ListProperty<IdBasedFetcher> idFetchers;
    private final ObjectProperty<IdBasedFetcher> idFetcher;
    private final Validator idFetcherValidator;
    private Task<Optional<BibEntry>> idLookupWorker;

    public NewEntryUnifiedViewModel(GuiPreferences preferences, LibraryTab libraryTab, DialogService dialogService, StateManager stateManager, UiTaskExecutor taskExecutor, FileUpdateMonitor fileUpdateMonitor) {
        this.preferences = preferences;
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.fileUpdateMonitor = fileUpdateMonitor;

        executing = new SimpleBooleanProperty(false);
        executedSuccessfully = new SimpleBooleanProperty(false);

        idText = new SimpleStringProperty();

        idTextValidator = new FunctionBasedValidator<>(
            idText,
            StringUtil::isNotBlank,
            ValidationMessage.error(Localization.lang("You must specify an identifier!")));

        idFetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
        idFetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences()));

        idFetcher = new SimpleObjectProperty<>();

        idFetcherValidator = new FunctionBasedValidator<>(
            idFetcher,
            Objects::nonNull,
            ValidationMessage.error(Localization.lang("You must select an identifier type!")));

        idLookupWorker = null;
    }

    public ReadOnlyBooleanProperty executingProperty() {
        return executing;
    }

    public ReadOnlyBooleanProperty executedSuccessfullyProperty() {
        return executedSuccessfully;
    }

    public StringProperty idTextProperty() {
        return idText;
    }

    public ReadOnlyBooleanProperty idTextValidatorProperty() {
        return idTextValidator.getValidationStatus().validProperty();
    }

    public ListProperty<IdBasedFetcher> idFetchersProperty() {
        return idFetchers;
    }

    public ObjectProperty<IdBasedFetcher> idFetcherProperty() {
        return idFetcher;
    }

    public ReadOnlyBooleanProperty idFetcherValidatorProperty() {
        return idFetcherValidator.getValidationStatus().validProperty();
    }

    private class IdLookupWorker extends Task<Optional<BibEntry>> {
        private String text = null;
        private IdBasedFetcher fetcher = null;

        @Override
        protected Optional<BibEntry> call() throws FetcherException {
            text = idText.getValue();
            fetcher = idFetcher.getValue();
            if (text == null || fetcher == null || text.isEmpty()) {
                return Optional.empty();
            }
            return fetcher.performSearchById(text);
        }
    }

    public void executeLookupIdentifier() {
        executing.setValue(true);
        idLookupWorker = new IdLookupWorker();

        idLookupWorker.setOnFailed(event -> {
            final Throwable exception = idLookupWorker.getException();
            final String exceptionMessage = exception.getMessage();
            final String textString = idText.getValue();
            final String fetcherName = idFetcher.getValue().getName();

            final String dialogTitle = Localization.lang("Failed to lookup identifier");

            if (exception instanceof FetcherClientException) {
                dialogService.showInformationDialogAndWait(
                    dialogTitle,
                    Localization.lang(
                        "Bibliographic data could not be retrieved.\n" +
                        "This is likely due to an issue with your input or network connection.\n" +
                        "Check your network connection and provided identifier, and try again.\n" +
                        exceptionMessage));
            } else if (exception instanceof FetcherServerException) {
                dialogService.showInformationDialogAndWait(
                    dialogTitle,
                    Localization.lang(
                        "Bibliographic data could not be retrieved.\n" +
                        "This is likely due to an issue being experienced by the server.\n" +
                        "Try again later.\n" +
                        exceptionMessage));
            } else {
                dialogService.showInformationDialogAndWait(
                    dialogTitle,
                    Localization.lang(
                        "Bibliographic data could not be retrieved.\n" +
                        "The following error was encountered:\n" +
                        "%0",
                        exceptionMessage));
            }

            LOGGER.error("An exception occurred with the '{}' fetcher when resolving '{}': '{}'.", idFetcher, idText, exception);

            executing.set(false);
        });

        idLookupWorker.setOnSucceeded(event -> {
            final Optional<BibEntry> result = idLookupWorker.getValue();
            if (result.isPresent()) {
                final ImportHandler handler = new ImportHandler(
                    libraryTab.getBibDatabaseContext(),
                    preferences,
                    fileUpdateMonitor,
                    libraryTab.getUndoManager(),
                    stateManager,
                    dialogService,
                    taskExecutor);
                handler.importEntryWithDuplicateCheck(libraryTab.getBibDatabaseContext(), result.get());
            } else {
                    dialogService.showWarningDialogAndWait(
                        Localization.lang("Invalid result returned"),
                        Localization.lang(
                            "Searching for the provided identifier succeeded, but an invalid result was returned.\n" +
                            "This entry may need to be added manually."));
            }

            executedSuccessfully.set(true);
        });

        taskExecutor.execute(idLookupWorker);
    }

    public void cancelAll() {
        if (idLookupWorker != null) {
            idLookupWorker.cancel();
        }
    }
}
