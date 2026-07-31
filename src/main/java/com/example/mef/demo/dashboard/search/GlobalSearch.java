package com.example.mef.demo.dashboard.search;


import com.example.mef.demo.Model.Field;
import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ctrl+K global search overlay: type-ahead across every registered module,
 * with arrow-key navigation and Enter/click to jump to the matching module.
 *
 * Extracted from DashboardController.openGlobalSearch() / closeSearch() /
 * highlightResult() / performSearch(), unchanged in behavior.
 */
public final class GlobalSearch {

    private final BorderPane rootPane;
    private final BorderPane contentPane;
    private final ModuleRegistry registry;
    private final DynamicDatabaseService dao;
    private final Consumer<Module> onModuleSelected;

    /** Kept as a field so a second Ctrl+K (or ESC) can find and remove it. */
    private StackPane searchOverlay;

    public GlobalSearch(BorderPane rootPane,
                        BorderPane contentPane,
                        ModuleRegistry registry,
                        DynamicDatabaseService dao,
                        Consumer<Module> onModuleSelected) {
        this.rootPane = rootPane;
        this.contentPane = contentPane;
        this.registry = registry;
        this.dao = dao;
        this.onModuleSelected = onModuleSelected;
    }

    /** Opens the overlay. No-op if it's already open. */
    public void open() {
        if (searchOverlay != null) return;

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 16px; -fx-padding: 0 4 0 12;");

        TextField searchField = new TextField();
        searchField.setPromptText(I18n.t("search.placeholder"));
        searchField.getStyleClass().add("search-popup-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label kbdEsc = new Label("ESC");
        kbdEsc.getStyleClass().add("search-kbd");
        kbdEsc.setStyle("-fx-padding: 3 6 3 6; -fx-margin: 0 8 0 0;");

        HBox searchRow = new HBox(4, searchIcon, searchField, kbdEsc);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(0, 8, 0, 0));

        Label hintLabel = new Label(I18n.t("search.hint"));
        hintLabel.getStyleClass().add("search-hint-label");
        hintLabel.setMaxWidth(Double.MAX_VALUE);

        VBox resultsList = new VBox(0);
        resultsList.setMaxHeight(320);

        ScrollPane resultsScroll = new ScrollPane(resultsList);
        resultsScroll.setFitToWidth(true);
        resultsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        resultsScroll.setMaxHeight(320);

        VBox popupContent = new VBox(0, searchRow, new Separator(), hintLabel);
        popupContent.getStyleClass().add("search-popup");

        final int[] selectedIndex = {-1};
        final List<Button>[] resultButtons = new List[]{new ArrayList<>()};

        searchField.textProperty().addListener((obs, old, query) -> {
            resultsList.getChildren().clear();
            resultButtons[0].clear();
            selectedIndex[0] = -1;
            hintLabel.setManaged(true);
            hintLabel.setVisible(true);

            if (query == null || query.isBlank()) {
                hintLabel.setText(I18n.t("search.hint"));
                popupContent.getChildren().remove(resultsScroll);
                return;
            }

            String needle = query.trim().toLowerCase();
            List<SearchResult> results = performSearch(needle);

            hintLabel.setText(results.size() + " " + I18n.t("search.results"));

            if (results.isEmpty()) {
                Label noRes = new Label(I18n.t("search.no_results") + " \"" + query + "\"");
                noRes.getStyleClass().add("search-hint-label");
                resultsList.getChildren().add(noRes);
            } else {
                for (SearchResult sr : results) {
                    Label moduleBadge = new Label(I18n.t(sr.moduleTitleKey()));
                    moduleBadge.getStyleClass().add("search-result-module");
                    Label mainText = new Label(sr.display());
                    mainText.getStyleClass().add("search-result-text");
                    Label subText = new Label(sr.sub());
                    subText.getStyleClass().add("search-result-sub");

                    VBox texts = new VBox(2, mainText, subText);
                    HBox.setHgrow(texts, Priority.ALWAYS);
                    HBox item = new HBox(10, moduleBadge, texts);
                    item.setAlignment(Pos.CENTER_LEFT);

                    Button btn = new Button();
                    btn.setGraphic(item);
                    btn.setMaxWidth(Double.MAX_VALUE);
                    btn.getStyleClass().add("search-result-item");
                    btn.setAlignment(Pos.CENTER_LEFT);
                    btn.setOnAction(ev -> {
                        close();
                        registry.all().stream()
                                .filter(m -> m.titleKey().equals(sr.moduleTitleKey()))
                                .findFirst()
                                .ifPresent(onModuleSelected);
                    });
                    resultButtons[0].add(btn);
                    resultsList.getChildren().add(btn);
                }
            }

            if (!popupContent.getChildren().contains(resultsScroll)) {
                popupContent.getChildren().add(resultsScroll);
            }
        });

        searchField.setOnKeyPressed(evt -> {
            List<Button> btns = resultButtons[0];
            if (evt.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (evt.getCode() == KeyCode.DOWN && !btns.isEmpty()) {
                selectedIndex[0] = Math.min(selectedIndex[0] + 1, btns.size() - 1);
                highlightResult(btns, selectedIndex[0]);
            } else if (evt.getCode() == KeyCode.UP && !btns.isEmpty()) {
                selectedIndex[0] = Math.max(selectedIndex[0] - 1, 0);
                highlightResult(btns, selectedIndex[0]);
            } else if (evt.getCode() == KeyCode.ENTER && selectedIndex[0] >= 0 && selectedIndex[0] < btns.size()) {
                btns.get(selectedIndex[0]).fire();
            }
        });

        searchOverlay = new StackPane(popupContent);
        searchOverlay.getStyleClass().add("search-overlay");
        searchOverlay.setAlignment(Pos.TOP_CENTER);
        searchOverlay.setPadding(new Insets(80, 0, 0, 0));
        searchOverlay.setOnMouseClicked(evt -> {
            if (evt.getTarget() == searchOverlay) close();
        });

        rootPane.setCenter(new StackPane(contentPane, searchOverlay));
        searchField.requestFocus();
    }

    private void highlightResult(List<Button> btns, int index) {
        for (int i = 0; i < btns.size(); i++) {
            btns.get(i).getStyleClass().removeAll("search-result-item-active");
            if (i == index) btns.get(i).getStyleClass().add("search-result-item-active");
        }
    }

    private void close() {
        if (searchOverlay == null) return;
        searchOverlay = null;
        rootPane.setCenter(contentPane);
    }

    private List<SearchResult> performSearch(String needle) {
        List<SearchResult> results = new ArrayList<>();
        for (Module module : registry.all()) {
            try {
                List<Map<String, String>> rows = dao.findAll(module.table(), module.columns(), module.orderBy());
                for (Map<String, String> row : rows) {
                    boolean matches = row.values().stream()
                            .filter(v -> v != null && !v.isBlank())
                            .anyMatch(v -> v.toLowerCase().contains(needle));
                    if (matches) {
                        List<String> parts = new ArrayList<>();
                        for (Field f : module.fields()) {
                            if ("password_hash".equals(f.column())) continue;
                            String v = row.get(f.column());
                            if (v != null && !v.isBlank()) parts.add(v);
                            if (parts.size() >= 2) break;
                        }
                        String display = String.join(" · ", parts);
                        String sub = "ID " + row.getOrDefault("id", "?");
                        results.add(new SearchResult(module.titleKey(), display, sub));
                        if (results.size() >= 20) return results;
                    }
                }
            } catch (Exception ignored) {}
        }
        return results;
    }

    private record SearchResult(String moduleTitleKey, String display, String sub) {}
}