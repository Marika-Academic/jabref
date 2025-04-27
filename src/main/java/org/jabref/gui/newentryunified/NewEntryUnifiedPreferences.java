package org.jabref.gui.newentryunified;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class NewEntryUnifiedPreferences {
    private final ObjectProperty<NewEntryUnifiedApproach> latestApproach;
    private final ObjectProperty<EntryType> latestInstantType;
    private final BooleanProperty idLookupGuessing;
    private final StringProperty latestIdFetcherName;
    private final StringProperty latestInterpretParserName;

    public NewEntryUnifiedPreferences() {
        this(NewEntryUnifiedApproach.CREATE_ENTRY,
        StandardEntryType.Article,
        true,
        null,
        PlainCitationParserChoice.RULE_BASED.getLocalizedName());
    }

    public NewEntryUnifiedPreferences(NewEntryUnifiedApproach approach,
                                      EntryType instantType,
                                      boolean idLookupGuessing,
                                      String idFetcherName,
                                      String interpretParserName) {
        this.latestApproach = new SimpleObjectProperty<>(approach);
        this.latestInstantType = new SimpleObjectProperty<>(instantType);
        this.idLookupGuessing = new SimpleBooleanProperty(idLookupGuessing);
        this.latestIdFetcherName = new SimpleStringProperty(idFetcherName);
        this.latestInterpretParserName = new SimpleStringProperty(interpretParserName);
    }

    public NewEntryUnifiedApproach getLatestApproach() {
        return latestApproach.get();
    }

    public void setLatestApproach(NewEntryUnifiedApproach approach) {
        latestApproach.set(approach);
    }

    public EntryType getLatestInstantType() {
        return latestInstantType.get();
    }

    public void setLatestInstantType(EntryType type) {
        latestInstantType.set(type);
    }

    public boolean getIdLookupGuessing() {
        return idLookupGuessing.get();
    }

    public void setIdLookupGuessing(boolean guessing) {
        idLookupGuessing.set(guessing);
    }

    public String getLatestIdFetcher() {
        return latestIdFetcherName.get();
    }

    public void setLatestIdFetcher(String idFetcherName) {
        latestIdFetcherName.set(idFetcherName);
    }

    public String getLatestInterpretParser() {
        return latestInterpretParserName.get();
    }

    public void setLatestInterpretParser(String interpretParserName) {
        latestInterpretParserName.set(interpretParserName);
    }
}
