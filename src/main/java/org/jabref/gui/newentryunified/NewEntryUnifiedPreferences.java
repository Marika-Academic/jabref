package org.jabref.gui.newentryunified;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class NewEntryUnifiedPreferences {
    private final ObjectProperty<NewEntryUnifiedApproach> latestApproach;
    private final ObjectProperty<EntryType> lastSelectedInstantType;
    private final BooleanProperty idLookupGuessing;

    public NewEntryUnifiedPreferences() {
        this(NewEntryUnifiedApproach.CREATE_ENTRY, StandardEntryType.Article, true);
    }

    public NewEntryUnifiedPreferences(NewEntryUnifiedApproach approach,
                                      EntryType instantType,
                                      boolean idLookupGuessing) {
        this.latestApproach = new SimpleObjectProperty<>(approach);
        this.lastSelectedInstantType = new SimpleObjectProperty<>(instantType);
        this.idLookupGuessing = new SimpleBooleanProperty(idLookupGuessing);
    }

    public NewEntryUnifiedApproach getLatestApproach() {
        return latestApproach.get();
    }

    public void setLatestApproach(NewEntryUnifiedApproach approach) {
        latestApproach.set(approach);
    }

    public EntryType getLastSelectedInstantType() {
        return lastSelectedInstantType.get();
    }

    public void setLastSelectedInstantType(EntryType type) {
        lastSelectedInstantType.set(type);
    }

    public boolean getIdLookupGuessing() {
        return idLookupGuessing.get();
    }

    public void setIdLookupGuessing(boolean guessing) {
        idLookupGuessing.set(guessing);
    }
}
