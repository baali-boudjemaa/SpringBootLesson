package com.example.mef.demo.dashboard.settings;

import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.util.BackupRestoreService;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;

/**
 * Handles the "backup database to file" / "restore database from file"
 * actions, extracted verbatim (behavior-wise) from
 * DashboardController.handleBackup / handleRestore.
 *
 * The FileChooser dialogs need an owner Window, which the original code
 * obtained from rootPane.getScene().getWindow() at call time — so that is
 * passed in per-call rather than stored, since it isn't available yet when
 * the controller's fields are wired up.
 */
public class BackupRestorePanel {

    private final BackupRestoreService backupRestoreService;

    public BackupRestorePanel(BackupRestoreService backupRestoreService) {
        this.backupRestoreService = backupRestoreService;
    }

    /** Opens a save dialog, then backs up the database to the chosen file. */
    public void backup(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("backup.choose_location"));
        chooser.setInitialFileName("rawdati_backup_" + LocalDate.now() + ".dump");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PostgreSQL Dump", "*.dump"));
        File target = chooser.showSaveDialog(owner);
        if (target == null) return;

        AsyncTasks.run(
                () -> backupRestoreService.backup(target),
                () -> DialogUtil.info(I18n.t("backup.title"), I18n.t("backup.success")),
                err -> DialogUtil.error(I18n.t("backup.title"), err.getMessage())
        );
    }

    /** Opens an open dialog (with confirmation), then restores the database from the chosen file. */
    public void restore(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("restore.choose_file"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PostgreSQL Dump", "*.dump"));
        File source = chooser.showOpenDialog(owner);
        if (source == null) return;

        if (!DialogUtil.confirm(I18n.t("restore.title"), I18n.t("restore.confirm_overwrite"))) {
            return;
        }

        AsyncTasks.run(
                () -> backupRestoreService.restore(source),
                () -> DialogUtil.info(I18n.t("restore.title"), I18n.t("restore.success")),
                err -> DialogUtil.error(I18n.t("restore.title"), err.getMessage())
        );
    }
}