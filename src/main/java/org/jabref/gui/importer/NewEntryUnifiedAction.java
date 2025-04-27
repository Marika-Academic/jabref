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

    private boolean isInstant;
    private Optional<EntryType> instantType;

    public NewEntryUnifiedAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.isInstant = false;
        this.instantType = Optional.empty();

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    public NewEntryUnifiedAction(EntryType instantType, Supplier<LibraryTab> tabSupplier, DialogService dialogService, GuiPreferences preferences, StateManager stateManager) {
        this(tabSupplier, dialogService, preferences, stateManager);

        this.isInstant = true;
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
        if (isInstant) {
            // If we're an instant action...
            final EntryType type;
            if (instantType.isPresent()) {
                // And we were created with an instant type, then we use that type.
                type = instantType.get();
            } else {
                // Otherwise, we query the last-selected entry type from the NewEntryUnified dialogue.
                type = preferences.getNewEntryUnifiedPreferences().getLatestInstantType();
            }
            // ...and create a new entry using this type.
            newEntry = new BibEntry(type);
        } else {
            // Otherwise, we launch a panel asking the user to specify details of the new entry.
            NewEntryUnifiedView newEntryDialog = new NewEntryUnifiedView(tabSupplier.get(), dialogService, preferences);
            newEntry = dialogService.showCustomDialogAndWait(newEntryDialog).orElse(null);
        }

        // This dialogue might handle inserting the new entry directly, so we don't do anything if the dialogue returns
        // `null`.
        if (newEntry != null) {
            tabSupplier.get().insertEntry(newEntry);
        }
    }
}
