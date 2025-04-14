package org.jabref.gui.newentryunified;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.stage.Screen;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.BiblatexAPAEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class NewEntryUnifiedView extends BaseDialog<BibEntry> {

    @FXML private ButtonType generateButton;
    @FXML private TitledPane entriesRecommendedTitle;
    @FXML private TitledPane entriesOtherTitle;
    @FXML private TitledPane entriesCustomTitle;
    @FXML private FlowPane entriesRecommendedPane;
    @FXML private FlowPane entriesOtherPane;
    @FXML private FlowPane entriesCustomPane;

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    private BibEntry result;

    public NewEntryUnifiedView(LibraryTab libraryTab, DialogService dialogService, GuiPreferences preferences) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.setTitle(Localization.lang("New entry"));
        ViewLoader.view(this).load().setAsDialogPane(this);

        setResultConverter(button -> { return result; });

        final Button btnGenerate = (Button) this.getDialogPane().lookupButton(generateButton);
        btnGenerate.getStyleClass().add("customGenerateButton");
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        entriesRecommendedTitle.managedProperty().bind(entriesRecommendedTitle.visibleProperty());
        entriesOtherTitle.managedProperty().bind(entriesOtherTitle.visibleProperty());
        entriesCustomTitle.managedProperty().bind(entriesCustomTitle.visibleProperty());

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
        addEntriesToPane(entriesRecommendedPane, recommendedEntries);
        addEntriesToPane(entriesOtherPane, otherEntries);
    }

    private void callbackEmptyEntrySelected(EntryType type) {
        result = new BibEntry(type);
        this.close();
    }

    private void addEntriesToPane(FlowPane pane, Collection<? extends BibEntryType> entries) {
        final double maxTooltipWidth = (2.0 / 3.0) * Screen.getPrimary().getBounds().getWidth();
        for (BibEntryType entry : entries) {
            final EntryType type = entry.getType();

            final Button button = new Button(type.getDisplayName());
            button.setUserData(entry);
            button.setOnAction(event -> callbackEmptyEntrySelected(type));
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
