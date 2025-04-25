package org.jabref.gui.importer;

import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.newentryunified.NewEntryUnifiedView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryUnifiedAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryUnifiedAction.class);

    private final Supplier<LibraryTab> tabSupplier;

    private final DialogService dialogService;

    private final GuiPreferences preferences;

    private Optional<EntryType> instantType;

    public NewEntryUnifiedAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.instantType = Optional.empty();

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    public NewEntryUnifiedAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager, EntryType instantType) {
        this(tabSupplier, dialogService, preferences, stateManager);
        this.instantType = Optional.ofNullable(instantType);
    }

    @Override
    public void execute() {
        // Without a tab supplier, we can only log an error message and abort.
        if (tabSupplier.get() == null) {
            LOGGER.error("Action 'New Entry' must be disabled when no database is open.");
            return;
        }

        BibEntry newEntry;
        if (instantType.isPresent()) {
            // If we were constructed with an instant type, then we simply create this type.
            newEntry = new BibEntry(instantType.get());
        }
        else
        {
            // Otherwise, we launch a panel asking the user to specify details of the new entry.
            NewEntryUnifiedView newEntryDialog = new NewEntryUnifiedView(tabSupplier.get(), dialogService, preferences);
            newEntry  = dialogService.showCustomDialogAndWait(newEntryDialog).orElse(null);
        }

        if (newEntry != null) {
            tabSupplier.get().insertEntry(newEntry);
        }
    }
}
