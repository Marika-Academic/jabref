package org.jabref.gui.newentryunified;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;

import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryUnifiedViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryUnifiedViewModel.class);

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    //private final Validator idTextValidator;
    private final ListProperty<IdBasedFetcher> idFetchers;

    public NewEntryUnifiedViewModel(LibraryTab libraryTab, DialogService dialogService, GuiPreferences preferences) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferences = preferences;

        idFetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
        idFetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences()));
    }

    public ListProperty<IdBasedFetcher> idFetchersProperty() {
        return idFetchers;
    }
}
