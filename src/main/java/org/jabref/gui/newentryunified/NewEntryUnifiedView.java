package org.jabref.gui.newentryunified;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.BiblatexAPAEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class NewEntryUnifiedView extends BaseDialog<BibEntry> {
    private final NewEntryUnifiedViewModel viewModel;

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final NewEntryUnifiedPreferences preferences;

    private final ControlsFxVisualizer visualizer;

    @FXML private ButtonType generateButtonType;
    private Button generateButton;

    @FXML private TabPane tabs;
    @FXML private Tab tabCreateEntry;
    @FXML private Tab tabLookupIdentifier;
    @FXML private Tab tabInterpretCitation;
    @FXML private Tab tabParseBibtex;

    @FXML private TitledPane entryRecommendedTitle;
    @FXML private FlowPane entryRecommended;
    @FXML private TitledPane entryOtherTitle;
    @FXML private FlowPane entryOther;
    @FXML private TitledPane entryCustomTitle;
    @FXML private FlowPane entryCustom;

    @FXML private TextField idText;
    @FXML private RadioButton idLookupGuess;
    @FXML private RadioButton idLookupSpecify;
    @FXML private ComboBox<IdBasedFetcher> idFetcher;

    private BibEntry result;

    public NewEntryUnifiedView(LibraryTab libraryTab, DialogService dialogService, GuiPreferences preferences) {
        viewModel = new NewEntryUnifiedViewModel(libraryTab, dialogService, preferences);

        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferences = preferences.getNewEntryUnifiedPreferences();

        visualizer = new ControlsFxVisualizer();
        this.setTitle(Localization.lang("New Entry"));
        ViewLoader.view(this).load().setAsDialogPane(this);

        generateButton = (Button) this.getDialogPane().lookupButton(generateButtonType);
        generateButton.getStyleClass().add("customGenerateButton");

        final Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setMinWidth(400);

        setResultConverter(button -> { return result; });

        finalizeTabs();
    }

    private void finalizeTabs() {
        switch (preferences.getSelectedApproach()) {
            case NewEntryUnifiedApproach.CREATE_ENTRY:
                tabs.getSelectionModel().select(tabCreateEntry);
                switchCreateEntry();
                break;
            case NewEntryUnifiedApproach.LOOKUP_IDENTIFIER:
                tabs.getSelectionModel().select(tabLookupIdentifier);
                switchLookupIdentifier();
                break;
            case NewEntryUnifiedApproach.INTERPRET_CITATION:
                tabs.getSelectionModel().select(tabInterpretCitation);
                switchInterpretCitation();
                break;
            case NewEntryUnifiedApproach.PARSE_BIBTEX:
                tabs.getSelectionModel().select(tabParseBibtex);
                switchParseBibtex();
                break;
        }

        tabCreateEntry.setOnSelectionChanged(event -> switchCreateEntry());
        tabLookupIdentifier.setOnSelectionChanged(event -> switchLookupIdentifier());
        tabInterpretCitation.setOnSelectionChanged(event -> switchInterpretCitation());
        tabParseBibtex.setOnSelectionChanged(event -> switchParseBibtex());
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        initializeCreateEntry();
        initializeLookupIdentifier();
    }

    private void initializeCreateEntry() {
        entryRecommendedTitle.managedProperty().bind(entryRecommendedTitle.visibleProperty());
        entryRecommended.managedProperty().bind(entryRecommended.visibleProperty());
        entryOtherTitle.managedProperty().bind(entryOtherTitle.visibleProperty());
        entryOther.managedProperty().bind(entryOther.visibleProperty());
        entryCustomTitle.managedProperty().bind(entryCustomTitle.visibleProperty());
        entryCustom.managedProperty().bind(entryCustom.visibleProperty());

        final boolean isBiblatexMode = libraryTab.getBibDatabaseContext().isBiblatexMode();

        List<BibEntryType> recommendedEntries;
        List<BibEntryType> otherEntries;
        if (isBiblatexMode) {
            recommendedEntries = BiblatexEntryTypeDefinitions.RECOMMENDED;
            otherEntries = new ArrayList<>(BiblatexEntryTypeDefinitions.ALL);
            otherEntries.removeAll(recommendedEntries);
            otherEntries.addAll(BiblatexSoftwareEntryTypeDefinitions.ALL);
            otherEntries.addAll(BiblatexAPAEntryTypeDefinitions.ALL);
        } else {
            recommendedEntries = BibtexEntryTypeDefinitions.RECOMMENDED;
            otherEntries = new ArrayList<>(BiblatexEntryTypeDefinitions.ALL);
            otherEntries.removeAll(recommendedEntries);
            otherEntries.addAll(IEEETranEntryTypeDefinitions.ALL);
        }
        addEntriesToPane(entryRecommended, recommendedEntries);
        addEntriesToPane(entryOther, otherEntries);

        final BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        final BibDatabaseMode customTypesDatabaseMode = isBiblatexMode ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX;
        final List<BibEntryType> customEntries = entryTypesManager.getAllCustomTypes(customTypesDatabaseMode);
        if (customEntries.isEmpty()) {
            entryCustomTitle.setVisible(false);
        } else {
            addEntriesToPane(entryCustom, customEntries);
        }
    }

    private void initializeLookupIdentifier() {
        idText.managedProperty().bind(idText.visibleProperty());
        idLookupGuess.managedProperty().bind(idLookupGuess.visibleProperty());
        idLookupSpecify.managedProperty().bind(idLookupSpecify.visibleProperty());
        idFetcher.managedProperty().bind(idFetcher.visibleProperty());
        idFetcher.disableProperty().bind(idLookupSpecify.selectedProperty().not());

        ToggleGroup toggleGroup = new ToggleGroup();
        idLookupGuess.setToggleGroup(toggleGroup);
        idLookupSpecify.setToggleGroup(toggleGroup);

        if (preferences.getIdLookupGuessing()) {
            idLookupGuess.selectedProperty().set(true);
        } else {
            idLookupSpecify.selectedProperty().set(true);
        }

        idLookupGuess.selectedProperty().addListener(
            new ChangeListener<Boolean>() {
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    preferences.setIdLookupGuessing(newValue);
                }
            });

        idFetcher.itemsProperty().bind(viewModel.idFetchersProperty());

        //new ViewModelListCellFactory<IdBasedFetcher>().withText(WebFetcher::getName).install(idFetcher);
    }

    @FXML
    private void switchCreateEntry() {
        if (!tabCreateEntry.isSelected()) {
            return;
        }

        preferences.setSelectedApproach(NewEntryUnifiedApproach.CREATE_ENTRY);
        if (generateButton != null) {
            generateButton.setDisable(true);
            generateButton.setText("Select");
        }
    }

    @FXML
    private void switchLookupIdentifier() {
        if (!tabLookupIdentifier.isSelected()) {
            return;
        }

        preferences.setSelectedApproach(NewEntryUnifiedApproach.LOOKUP_IDENTIFIER);
        if (generateButton != null) {
            generateButton.setDisable(false);
            generateButton.setText("Lookup");
        }
    }

    @FXML
    private void switchInterpretCitation() {
        if (!tabInterpretCitation.isSelected()) {
            return;
        }

        preferences.setSelectedApproach(NewEntryUnifiedApproach.INTERPRET_CITATION);
        if (generateButton != null) {
            generateButton.setDisable(false);
            generateButton.setText("Interpret");
        }
    }

    @FXML
    private void switchParseBibtex() {
        if (!tabParseBibtex.isSelected()) {
            return;
        }

        preferences.setSelectedApproach(NewEntryUnifiedApproach.PARSE_BIBTEX);
        if (generateButton != null) {
            generateButton.setDisable(false);
            generateButton.setText("Parse");
        }
    }

    private void callbackEntryTypeSelected(EntryType type) {
        result = new BibEntry(type);
        this.close();
    }

    private void addEntriesToPane(FlowPane pane, Collection<? extends BibEntryType> entries) {
        final double maxTooltipWidth = (2.0 / 3.0) * Screen.getPrimary().getBounds().getWidth();
        for (BibEntryType entry : entries) {
            final EntryType type = entry.getType();

            final Button button = new Button(type.getDisplayName());
            button.setUserData(entry);
            button.setOnAction(event -> callbackEntryTypeSelected(type));
            pane.getChildren().add(button);

            final String description = descriptionOfEntryType(type);
            if (description != null) {
                final Tooltip tooltip = new Tooltip(description);
                tooltip.setMaxWidth(maxTooltipWidth);
                tooltip.setWrapText(true);
                button.setTooltip(tooltip);
            }
        }
    }

    public static String descriptionOfEntryType(EntryType type) {
        if (type instanceof StandardEntryType entryType) {
            return descriptionOfStandardEntryType(entryType);
        }
        return null;
    }

    public static String descriptionOfStandardEntryType(StandardEntryType type) {
        // These descriptions are taken from subsection 2.1 of the biblatex package documentation.
        // Biblatex is a superset of bibtex, with more elaborate descriptions, so its documentation is preferred.
        // See [https://mirrors.ibiblio.org/pub/mirrors/CTAN/macros/latex/contrib/biblatex/doc/biblatex.pdf].
        return switch (type) {
            case Article        -> Localization.lang("An article in a journal, magazine, newspaper, or other periodical which forms a self-contained unit with its own title.");
            case Book           -> Localization.lang("A single-volume book with one or more authors where the authors share credit for the work as a whole.");
            case Booklet        -> Localization.lang("A book-like work without a formal publisher or sponsoring institution.");
            case Collection     -> Localization.lang("A single-volume collection with multiple, self-contained contributions by distinct authors which have their own title. The work as a whole has no overall author but it will usually have an editor.");
            case Conference     -> Localization.lang("A legacy alias for \"InProceedings\".");
            case InBook         -> Localization.lang("A part of a book which forms a self-contained unit with its own title.");
            case InCollection   -> Localization.lang("A contribution to a collection which forms a self-contained unit with a distinct author and title.");
            case InProceedings  -> Localization.lang("An article in a conference proceedings.");
            case Manual         -> Localization.lang("Technical or other documentation, not necessarily in printed form.");
            case MastersThesis  -> Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term  Master's thesis.");
            case Misc           -> Localization.lang("A fallback type for entries which do not fit into any other category.");
            case PhdThesis      -> Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term PhD thesis.");
            case Proceedings    -> Localization.lang("A single-volume conference proceedings. This type is very similar to \"Collection\".");
            case TechReport     -> Localization.lang("Similar to \"Report\" except that the type field is optional and defaults to the localised term technical report.");
            case Unpublished    -> Localization.lang("A work with an author and a title which has not been formally published, such as a manuscript or the script of a talk.");
            case BookInBook     -> Localization.lang("This type is similar to \"InBook\" but intended for works originally published as a stand-alone book.");
            case InReference    -> Localization.lang("An article in a work of reference. This is a more specific variant of the generic \"InCollection\" entry type.");
            case MvBook         -> Localization.lang("A multi-volume \"Book\".");
            case MvCollection   -> Localization.lang("A multi-volume \"Collection\".");
            case MvProceedings  -> Localization.lang("A multi-volume \"Proceedings\" entry.");
            case MvReference    -> Localization.lang("A multi-volume \"Reference\" entry. The standard styles will treat this entry type as an alias for \"MvCollection\".");
            case Online         -> Localization.lang("This entry type is intended for sources such as web sites which are intrinsically online resources.");
            case Reference      -> Localization.lang("A single-volume work of reference such as an encyclopedia or a dictionary.");
            case Report         -> Localization.lang("A technical report, research report, or white paper published by a university or some other institution.");
            case Set            -> Localization.lang("An entry set is a group of entries which are cited as a single reference and listed as a single item in the bibliography.");
            case SuppBook       -> Localization.lang("Supplemental material in a \"Book\". This type is provided for elements such as prefaces, introductions, forewords, afterwords, etc. which often have a generic title only.");
            case SuppCollection -> Localization.lang("Supplemental material in a \"Collection\".");
            case SuppPeriodical -> Localization.lang("Supplemental material in a \"Periodical\". This type may be useful when referring to items such as regular columns, obituaries, letters to the editor, etc. which only have a generic title.");
            case Thesis         -> Localization.lang("A thesis written for an educational institution to satisfy the requirements for a degree.");
            case WWW            -> Localization.lang("An alias for \"Online\", provided for jurabib compatibility.");
            case Software       -> Localization.lang("Computer software. The standard styles will treat this entry type as an alias for \"Misc\".");
            case Dataset        -> Localization.lang("A data set or a similar collection of (mostly) raw data.");
        };
    }
}
