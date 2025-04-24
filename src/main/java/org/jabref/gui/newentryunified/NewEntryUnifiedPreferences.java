package org.jabref.gui.newentryunified;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class NewEntryUnifiedPreferences {
    private final ObjectProperty<NewEntryUnifiedApproach> selectedApproach;
    private final BooleanProperty idLookupGuessing;

    public static NewEntryUnifiedPreferences defaults() {
        return new NewEntryUnifiedPreferences(NewEntryUnifiedApproach.CREATE_ENTRY, true);
    }

    public NewEntryUnifiedPreferences(NewEntryUnifiedApproach selectedApproach,
                                      boolean idLookupGuessing) {
        this.selectedApproach = new SimpleObjectProperty<>(selectedApproach);
        this.idLookupGuessing = new SimpleBooleanProperty(idLookupGuessing);
    }

    public NewEntryUnifiedApproach getSelectedApproach() {
        return selectedApproach.get();
    }

    public void setSelectedApproach(NewEntryUnifiedApproach approach) {
        selectedApproach.set(approach);
    }

    public boolean getIdLookupGuessing() {
        return idLookupGuessing.get();
    }

    public void setIdLookupGuessing(boolean guessing) {
        idLookupGuessing.set(guessing);
    }
}
