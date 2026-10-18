package com.footballgenie.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.*;
import android.widget.FrameLayout;

import java.util.Locale;
import java.util.Random;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.widget.Toast;
import android.app.AlertDialog;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private EditText homeInput, awayInput, homeGoals, awayGoals;
    private TextView result, status;
    private LinearLayout statsSummary;
    private TextView homeWinValue, drawValue, awayWinValue;
    private TextView overValue, underValue, bttsValue;
    // Home-screen fixture card must reflect the same selected fixture as Predict.
    private TextView homeDisplayName, awayDisplayName, homeDisplayLeague;
    private LinearLayout mainLayout;
    private FrameLayout screenHost;
    private final View[] appScreens = new View[8];
    private TextView simulationSummary;
    private JSONObject selectedFixture;
    private LinearLayout fixtureListLayout;
    private LinearLayout fixtureFiltersLayout;
    private TextView fixtureListStatus;
    private final ArrayList<JSONObject> loadedFixtures = new ArrayList<>();
    private String activeFixtureFilter = "ALL";

    private final int BG = Color.rgb(9, 16, 31);
    private final int CARD = Color.rgb(20, 31, 51);
    private final int GREEN = Color.rgb(80, 230, 150);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(170, 185, 205);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        screenHost = new FrameLayout(this);
        LinearLayout.LayoutParams hostParams =
            new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(screenHost, hostParams);

        appScreens[0] = createAppPage(0); // Home
        appScreens[1] = createAppPage(1); // Match Centre
        appScreens[2] = createAppPage(2); // Prediction Results
        appScreens[3] = createAppPage(3); // Match Simulation
        appScreens[4] = createAppPage(4); // Prediction History
        appScreens[5] = createAppPage(5); // Live Fixtures
        appScreens[6] = createAppPage(6); // Validation & Performance
        appScreens[7] = createAppPage(7); // Settings

        root.addView(buildBottomNavigation(),
            new LinearLayout.LayoutParams(-1, dp(62)));
        setContentView(root);
        showScreen(0);
        checkApiConnection();
    }

    private View createAppPage(int screen) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(page);

        LinearLayout previousLayout = mainLayout;
        mainLayout = page;
        switch (screen) {
            case 0:
                buildHeader();
                buildHomeShortcuts();
                break;
            case 1:
                buildMatchForm();
                break;
            case 2:
                buildResults();
                break;
            case 3:
                buildSimulationScreen();
                break;
            case 4:
                buildHistoryTools();
                break;
            case 5:
                buildLiveFixturesScreen();
                break;
            case 6:
                buildValidationScreen();
                break;
            case 7:
                buildSettingsScreen();
                break;
        }
        mainLayout = previousLayout;
        return scroll;
    }

    private LinearLayout buildBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(5), dp(4), dp(6));
        nav.setBackground(rounded(Color.rgb(5, 13, 26), 0));

        String[] icons = {"⌂", "⚽", "▥", "◉", "◷"};
        String[] labels = {"HOME", "PREDICT", "RESULTS", "SIMULATE", "HISTORY"};
        int[] targets = {0, 1, 2, 3, 4};

        for (int i = 0; i < labels.length; i++) {
            final int target = targets[i];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(5), dp(2), dp(4));
            item.setBackground(rounded(Color.rgb(5, 13, 26), 10));
            item.setClickable(true);
            item.setFocusable(true);

            TextView icon = label(icons[i]);
            icon.setTextColor(GREEN);
            icon.setTextSize(22);
            icon.setGravity(Gravity.CENTER);
            item.addView(icon, new LinearLayout.LayoutParams(-1, dp(28)));

            TextView title = label(labels[i]);
            title.setTextColor(WHITE);
            title.setTextSize(10);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            item.addView(title, new LinearLayout.LayoutParams(-1, dp(18)));

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(62), 1f);
            params.setMargins(dp(2), 0, dp(2), 0);
            nav.addView(item, params);
            item.setOnClickListener(v -> showScreen(target));
        }
        return nav;
    }

    private void showScreen(int screen) {
        if (screen < 0 || screen >= appScreens.length || appScreens[screen] == null) return;
        screenHost.removeAllViews();
        screenHost.addView(appScreens[screen],
            new FrameLayout.LayoutParams(-1, -1));
        if (screen == 3 && simulationSummary != null && result != null) {
            simulationSummary.setText(result.getText());
        }
    }

    private void buildHomeShortcuts() {
        addHeading("⚽", "MATCH CENTRE");
        TextView subtitle = label("Select a match or load live fixtures");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(12);
        subtitle.setPadding(dp(2), 0, 0, dp(12));
        mainLayout.addView(subtitle);

        Button loadFixturesButton = new Button(this);
        loadFixturesButton.setText("🌐  LOAD LIVE FIXTURES");
        loadFixturesButton.setTextColor(Color.rgb(4, 25, 20));
        loadFixturesButton.setTextSize(14);
        loadFixturesButton.setTypeface(null, Typeface.BOLD);
        loadFixturesButton.setAllCaps(false);
        loadFixturesButton.setBackground(rounded(GREEN, 12));
        LinearLayout.LayoutParams loadParams = new LinearLayout.LayoutParams(-1, dp(50));
        loadParams.bottomMargin = dp(12);
        mainLayout.addView(loadFixturesButton, loadParams);
        loadFixturesButton.setOnClickListener(v -> {
            showScreen(5);
            loadFixtures();
        });

        LinearLayout fixtureCard = card();
        fixtureCard.setOrientation(LinearLayout.VERTICAL);
        homeDisplayLeague = label("🏆  YOUR NEXT PREDICTION");
        homeDisplayLeague.setTextColor(GREEN);
        homeDisplayLeague.setTextSize(11);
        homeDisplayLeague.setTypeface(null, Typeface.BOLD);
        fixtureCard.addView(homeDisplayLeague);

        LinearLayout teams = new LinearLayout(this);
        teams.setOrientation(LinearLayout.HORIZONTAL);
        teams.setGravity(Gravity.CENTER_VERTICAL);
        teams.setPadding(0, dp(14), 0, dp(12));

        LinearLayout homeTeam = new LinearLayout(this);
        homeTeam.setOrientation(LinearLayout.VERTICAL);
        homeTeam.setGravity(Gravity.CENTER);
        TextView homeBadge = label("⚽");
        homeBadge.setTextSize(28);
        homeBadge.setGravity(Gravity.CENTER);
        homeTeam.addView(homeBadge);
        homeDisplayName = label("Home Team");
        homeDisplayName.setTextColor(WHITE);
        homeDisplayName.setTextSize(12);
        homeDisplayName.setTypeface(null, Typeface.BOLD);
        homeDisplayName.setGravity(Gravity.CENTER);
        homeDisplayName.setMaxLines(2);
        homeDisplayName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        homeTeam.addView(homeDisplayName);
        teams.addView(homeTeam, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView vs = label("VS");
        vs.setTextColor(GREEN);
        vs.setTextSize(20);
        vs.setTypeface(null, Typeface.BOLD);
        vs.setGravity(Gravity.CENTER);
        teams.addView(vs, new LinearLayout.LayoutParams(dp(48), -2));

        LinearLayout awayTeam = new LinearLayout(this);
        awayTeam.setOrientation(LinearLayout.VERTICAL);
        awayTeam.setGravity(Gravity.CENTER);
        TextView awayBadge = label("⚽");
        awayBadge.setTextSize(28);
        awayBadge.setGravity(Gravity.CENTER);
        awayTeam.addView(awayBadge);
        awayDisplayName = label("Away Team");
        awayDisplayName.setTextColor(WHITE);
        awayDisplayName.setTextSize(12);
        awayDisplayName.setTypeface(null, Typeface.BOLD);
        awayDisplayName.setGravity(Gravity.CENTER);
        awayDisplayName.setMaxLines(2);
        awayDisplayName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        awayTeam.addView(awayDisplayName);
        teams.addView(awayTeam, new LinearLayout.LayoutParams(0, -2, 1f));
        fixtureCard.addView(teams);

        TextView fixtureHelp = label("Choose a live fixture to fill these teams automatically, or enter a match manually.");
        fixtureHelp.setTextColor(MUTED);
        fixtureHelp.setTextSize(11);
        fixtureHelp.setGravity(Gravity.CENTER);
        fixtureHelp.setPadding(0, 0, 0, dp(10));
        fixtureCard.addView(fixtureHelp);

        Button predictButton = new Button(this);
        predictButton.setText("▶  SIMULATE 10,000 MATCHES");
        predictButton.setTextColor(Color.rgb(4, 25, 20));
        predictButton.setTextSize(13);
        predictButton.setTypeface(null, Typeface.BOLD);
        predictButton.setAllCaps(false);
        predictButton.setBackground(rounded(GREEN, 12));
        fixtureCard.addView(predictButton, new LinearLayout.LayoutParams(-1, dp(50)));
        predictButton.setOnClickListener(v -> showScreen(1));
        mainLayout.addView(fixtureCard);

        addHeading("✦", "QUICK ACCESS");
        LinearLayout rowOne = new LinearLayout(this);
        rowOne.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.addView(rowOne);
        addHomeTile(rowOne, "🌐", "Live Fixtures", "Get latest matches", 5);
        addHomeTile(rowOne, "📋", "My Predictions", "View saved picks", 4);

        LinearLayout rowTwo = new LinearLayout(this);
        rowTwo.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.addView(rowTwo);
        addHomeTile(rowTwo, "✓", "Validation", "Accuracy & performance", 6);
        addHomeTile(rowTwo, "◷", "History", "Past results & insights", 4);
    }

    private void addHomeTile(LinearLayout row, String icon, String title,
            String detail, int target) {
        LinearLayout tile = card();
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(12), dp(13), dp(10), dp(13));
        TextView iconView = label(icon);
        iconView.setTextColor(GREEN);
        iconView.setTextSize(22);
        tile.addView(iconView);
        TextView titleView = label(title);
        titleView.setTextColor(WHITE);
        titleView.setTextSize(12);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, dp(5), 0, dp(3));
        tile.addView(titleView);
        TextView detailView = label(detail);
        detailView.setTextColor(MUTED);
        detailView.setTextSize(10);
        tile.addView(detailView);
        tile.setClickable(true);
        tile.setOnClickListener(v -> showScreen(target));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(dp(3), 0, dp(3), dp(6));
        row.addView(tile, params);
    }

    private void addShortcut(String title, String description, int target) {
        LinearLayout shortcut = card();
        shortcut.setOrientation(LinearLayout.VERTICAL);
        TextView heading = label(title);
        heading.setTextColor(GREEN);
        heading.setTextSize(14);
        heading.setTypeface(null, Typeface.BOLD);
        shortcut.addView(heading);
        TextView detail = label(description);
        detail.setTextColor(MUTED);
        detail.setTextSize(12);
        detail.setPadding(0, dp(5), 0, dp(2));
        shortcut.addView(detail);
        shortcut.setClickable(true);
        shortcut.setFocusable(true);
        shortcut.setOnClickListener(v -> showScreen(target));
        mainLayout.addView(shortcut);
    }

    private void buildSimulationScreen() {
        addHeading("04", "MATCH SIMULATION");
        TextView note = label("This screen presents the latest match simulation and its modelled match-flow analysis. Run a prediction from Match Centre first.");
        note.setTextColor(MUTED);
        note.setTextSize(13);
        note.setLineSpacing(dp(3), 1.0f);
        note.setPadding(0, 0, 0, dp(12));
        mainLayout.addView(note);

        LinearLayout simulationCard = card();
        simulationCard.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("LATEST SIMULATED MATCH");
        title.setTextColor(GREEN);
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        simulationCard.addView(title);

        simulationSummary = label("No simulation yet. Open MATCH CENTRE, enter both teams and expected goals, then run 10,000 simulations.");
        simulationSummary.setTextColor(WHITE);
        simulationSummary.setTextSize(13);
        simulationSummary.setLineSpacing(dp(4), 1.0f);
        simulationSummary.setPadding(0, dp(10), 0, dp(8));
        simulationCard.addView(simulationSummary);

        Button predictButton = new Button(this);
        predictButton.setText("⚽  RUN / UPDATE SIMULATION");
        predictButton.setTextColor(Color.rgb(8, 25, 22));
        predictButton.setTextSize(13);
        predictButton.setAllCaps(false);
        predictButton.setTypeface(null, Typeface.BOLD);
        predictButton.setBackground(rounded(GREEN, 13));
        simulationCard.addView(predictButton, new LinearLayout.LayoutParams(-1, dp(50)));
        predictButton.setOnClickListener(v -> showScreen(1));
        mainLayout.addView(simulationCard);
    }

    private void buildLiveFixturesScreen() {
        addHeading("05", "LIVE FIXTURES");
        TextView note = label("Browse matches by competition and kickoff. Tap any fixture card to select it for your prediction.");
        note.setTextColor(MUTED);
        note.setTextSize(13);
        note.setLineSpacing(dp(3), 1.0f);
        note.setPadding(0, 0, 0, dp(12));
        mainLayout.addView(note);

        LinearLayout toolbar = card();
        toolbar.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView title = label("MATCH SCHEDULE");
        title.setTextColor(GREEN);
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        toolbar.addView(title);
        fixtureListStatus = label("Load the latest fixtures to see the schedule here.");
        fixtureListStatus.setTextColor(MUTED);
        fixtureListStatus.setTextSize(12);
        fixtureListStatus.setPadding(0, dp(5), 0, dp(10));
        toolbar.addView(fixtureListStatus);
        Button load = new Button(this);
        load.setText("↻  REFRESH FIXTURES");
        load.setTextColor(Color.rgb(8, 25, 22));
        load.setTextSize(13);
        load.setTypeface(null, Typeface.BOLD);
        load.setAllCaps(false);
        load.setBackground(rounded(GREEN, 13));
        toolbar.addView(load, new LinearLayout.LayoutParams(-1, dp(48)));
        load.setOnClickListener(v -> loadFixtures());
        mainLayout.addView(toolbar);

        fixtureFiltersLayout = new LinearLayout(this);
        fixtureFiltersLayout.setOrientation(LinearLayout.HORIZONTAL);
        fixtureFiltersLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, dp(44));
        filterParams.topMargin = dp(12);
        mainLayout.addView(fixtureFiltersLayout, filterParams);
        buildFixtureFilterTabs();

        fixtureListLayout = new LinearLayout(this);
        fixtureListLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, -2);
        listParams.topMargin = dp(10);
        mainLayout.addView(fixtureListLayout, listParams);

        addFixtureEmptyState("Your fixture schedule will appear here after refresh.");
    }

    private void buildFixtureFilterTabs() {
        if (fixtureFiltersLayout == null) return;
        fixtureFiltersLayout.removeAllViews();
        String[] filters = {"FINISHED", "LIVE", "UPCOMING", "ALL"};
        for (String filter : filters) {
            TextView tab = label(filter);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(10);
            tab.setSingleLine(true);
            tab.setTypeface(null, Typeface.BOLD);
            boolean selected = filter.equals(activeFixtureFilter);
            tab.setTextColor(selected ? Color.rgb(7, 23, 20) : MUTED);
            tab.setBackground(rounded(selected ? GREEN : CARD, 10));
            tab.setPadding(dp(2), 0, dp(2), 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
            params.setMargins(dp(3), 0, dp(3), 0);
            fixtureFiltersLayout.addView(tab, params);
            tab.setOnClickListener(v -> {
                activeFixtureFilter = filter;
                buildFixtureFilterTabs();
                renderFixtureCards(loadedFixtures);
            });
        }
    }

    private boolean fixtureMatchesFilter(JSONObject match) {
        String status = match.optString("status", "SCHEDULED");
        boolean live = "IN_PLAY".equals(status) || "PAUSED".equals(status)
            || "EXTRA_TIME".equals(status) || "PENALTY_SHOOTOUT".equals(status);
        boolean finished = "FINISHED".equals(status) || "AWARDED".equals(status);
        if ("FINISHED".equals(activeFixtureFilter)) return finished;
        if ("LIVE".equals(activeFixtureFilter)) return live;
        if ("UPCOMING".equals(activeFixtureFilter)) {
            return !live && !finished && !"POSTPONED".equals(status)
                && !"SUSPENDED".equals(status) && !"CANCELLED".equals(status);
        }
        return true;
    }

    private void addFixtureEmptyState(String message) {
        if (fixtureListLayout == null) return;
        fixtureListLayout.removeAllViews();
        LinearLayout empty = card();
        TextView text = label("⚽  " + message);
        text.setTextColor(MUTED);
        text.setTextSize(13);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(8), dp(18), dp(8), dp(18));
        empty.addView(text);
        fixtureListLayout.addView(empty);
    }

    private String fixtureStatusLabel(String status) {
        switch (status) {
            case "IN_PLAY": return "● LIVE";
            case "PAUSED": return "● HALF-TIME";
            case "EXTRA_TIME": return "● EXTRA TIME";
            case "PENALTY_SHOOTOUT": return "● PENALTIES";
            case "FINISHED": return "FULL-TIME";
            case "POSTPONED": return "POSTPONED";
            case "SUSPENDED": return "SUSPENDED";
            case "CANCELLED": return "CANCELLED";
            case "AWARDED": return "RESULT AWARDED";
            default: return "UPCOMING";
        }
    }

    private void renderFixtureCards(ArrayList<JSONObject> fixtures) {
        if (fixtureListLayout == null) return;
        fixtureListLayout.removeAllViews();
        ArrayList<JSONObject> visibleFixtures = new ArrayList<>();
        for (JSONObject fixture : fixtures) {
            if (fixtureMatchesFilter(fixture)) visibleFixtures.add(fixture);
        }
        if (visibleFixtures.isEmpty()) {
            String message = fixtures.isEmpty()
                ? "No fixtures are available for the current date range."
                : "No " + activeFixtureFilter.toLowerCase() + " matches in the loaded fixtures.";
            addFixtureEmptyState(message);
            if (fixtureListStatus != null) {
                fixtureListStatus.setText(fixtures.size() + " total fixtures  •  0 " + activeFixtureFilter.toLowerCase());
            }
            return;
        }

        int shown = 0;
        for (JSONObject match : visibleFixtures) {
            JSONObject home = match.optJSONObject("homeTeam");
            JSONObject away = match.optJSONObject("awayTeam");
            if (home == null || away == null) continue;

            String homeName = home.optString("name", "Home team");
            String awayName = away.optString("name", "Away team");
            String status = match.optString("status", "SCHEDULED");
            String kickoff = match.optString("utcDate", "");
            JSONObject competition = match.optJSONObject("competition");
            String competitionName = competition == null ? "" : competition.optString("name", "");
            JSONObject score = match.optJSONObject("score");
            JSONObject fullTime = score == null ? null : score.optJSONObject("fullTime");
            String scoreText = "";
            if (fullTime != null && !fullTime.isNull("home") && !fullTime.isNull("away")) {
                scoreText = fullTime.optInt("home") + "  -  " + fullTime.optInt("away");
            } else if ("IN_PLAY".equals(status) || "PAUSED".equals(status)
                    || "EXTRA_TIME".equals(status) || "PENALTY_SHOOTOUT".equals(status)) {
                JSONObject current = score == null ? null : score.optJSONObject("regularTime");
                if (current != null && !current.isNull("home") && !current.isNull("away")) {
                    scoreText = current.optInt("home") + "  -  " + current.optInt("away");
                } else {
                    scoreText = "LIVE";
                }
            }

            LinearLayout fixtureCard = card();
            fixtureCard.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(10);
            fixtureListLayout.addView(fixtureCard, cardParams);

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView league = label(competitionName.isEmpty() ? "FOOTBALL FIXTURE" : competitionName.toUpperCase());
            league.setTextColor(GREEN);
            league.setTextSize(10);
            league.setTypeface(null, Typeface.BOLD);
            top.addView(league, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView statusView = label(fixtureStatusLabel(status));
            statusView.setTextColor(("IN_PLAY".equals(status) || "PAUSED".equals(status)
                    || "EXTRA_TIME".equals(status) || "PENALTY_SHOOTOUT".equals(status)) ? GREEN : MUTED);
            statusView.setTextSize(10);
            statusView.setTypeface(null, Typeface.BOLD);
            top.addView(statusView);
            fixtureCard.addView(top);

            if (kickoff.length() >= 16) {
                TextView date = label(kickoff.substring(0, 10) + "  •  " + kickoff.substring(11, 16) + " UTC");
                date.setTextColor(MUTED);
                date.setTextSize(11);
                date.setPadding(0, dp(6), 0, dp(10));
                fixtureCard.addView(date);
            }

            LinearLayout teams = new LinearLayout(this);
            teams.setOrientation(LinearLayout.HORIZONTAL);
            teams.setGravity(Gravity.CENTER_VERTICAL);
            TextView homeView = label(homeName);
            homeView.setTextColor(WHITE);
            homeView.setTextSize(13);
            homeView.setTypeface(null, Typeface.BOLD);
            homeView.setGravity(Gravity.CENTER);
            TextView vs = label(scoreText.isEmpty() ? "VS" : scoreText);
            vs.setTextColor(GREEN);
            vs.setTextSize(12);
            vs.setTypeface(null, Typeface.BOLD);
            vs.setGravity(Gravity.CENTER);
            TextView awayView = label(awayName);
            awayView.setTextColor(WHITE);
            awayView.setTextSize(13);
            awayView.setTypeface(null, Typeface.BOLD);
            awayView.setGravity(Gravity.CENTER);
            teams.addView(homeView, new LinearLayout.LayoutParams(0, -2, 1f));
            teams.addView(vs, new LinearLayout.LayoutParams(dp(62), -2));
            teams.addView(awayView, new LinearLayout.LayoutParams(0, -2, 1f));
            fixtureCard.addView(teams);

            TextView choose = label("TAP TO SELECT  →");
            choose.setTextColor(MUTED);
            choose.setTextSize(10);
            choose.setGravity(Gravity.RIGHT);
            choose.setPadding(0, dp(10), 0, 0);
            fixtureCard.addView(choose);

            fixtureCard.setClickable(true);
            fixtureCard.setFocusable(true);
            fixtureCard.setOnClickListener(v -> selectFixture(match));
            shown++;
        }
        if (fixtureListStatus != null) {
            fixtureListStatus.setText(shown + " " + activeFixtureFilter.toLowerCase()
                + " fixtures shown  •  " + fixtures.size() + " total");
        }
    }

    private void selectFixture(JSONObject selected) {
        try {
            JSONObject homeTeamObject = selected.optJSONObject("homeTeam");
            JSONObject awayTeamObject = selected.optJSONObject("awayTeam");
            String homeName = homeTeamObject == null ? "" : homeTeamObject.optString("name", "");
            String awayName = awayTeamObject == null ? "" : awayTeamObject.optString("name", "");
            if (homeName.trim().isEmpty() && homeTeamObject != null) {
                homeName = homeTeamObject.optString("shortName", homeTeamObject.optString("tla", ""));
            }
            if (awayName.trim().isEmpty() && awayTeamObject != null) {
                awayName = awayTeamObject.optString("shortName", awayTeamObject.optString("tla", ""));
            }
            if (homeName.trim().isEmpty() || awayName.trim().isEmpty()) {
                Toast.makeText(this, "Team names are missing from this fixture. Refresh and try another.", Toast.LENGTH_LONG).show();
                return;
            }
            selectedFixture = selected;
            homeInput.setText(homeName);
            awayInput.setText(awayName);

            if (homeDisplayName != null) homeDisplayName.setText(homeName.isEmpty() ? "Home Team" : homeName);
            if (awayDisplayName != null) awayDisplayName.setText(awayName.isEmpty() ? "Away Team" : awayName);
            if (homeDisplayLeague != null) {
                JSONObject competition = selected.optJSONObject("competition");
                String competitionName = competition == null ? "" : competition.optString("name", "");
                homeDisplayLeague.setText(competitionName.isEmpty()
                    ? "🏆  YOUR NEXT PREDICTION" : "🏆  " + competitionName);
            }

            if (statsSummary != null) {
                renderRecentFormMessage("LOADING RECENT FORM", "Loading completed matches and team statistics...");
            }
            loadTeamStatsForFixture(selected);
            showScreen(1);
            Toast.makeText(this, "Teams loaded: " + homeName + " vs " + awayName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not select this fixture.", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildValidationScreen() {
        addHeading("06", "VALIDATION & PERFORMANCE");
        TextView note = label("Use finished matches to evaluate the model. Accuracy and calibration metrics should be based on saved predictions with known final scores—not on predictions that are still pending.");
        note.setTextColor(MUTED);
        note.setTextSize(13);
        note.setLineSpacing(dp(3), 1.0f);
        note.setPadding(0, 0, 0, dp(14));
        mainLayout.addView(note);

        LinearLayout validationCard = card();
        TextView title = label("MODEL PERFORMANCE REPORT");
        title.setTextColor(GREEN);
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        validationCard.addView(title);
        TextView detail = label("Open prediction history to update actual scores and inspect evaluated predictions, goal prediction error, probability calibration and baseline comparisons.");
        detail.setTextColor(MUTED);
        detail.setTextSize(12);
        detail.setLineSpacing(dp(3), 1.0f);
        detail.setPadding(0, dp(6), 0, dp(12));
        validationCard.addView(detail);
        Button open = new Button(this);
        open.setText("📈  OPEN VALIDATION REPORT");
        open.setTextColor(WHITE);
        open.setTextSize(13);
        open.setAllCaps(false);
        open.setBackground(rounded(CARD, 13));
        validationCard.addView(open, new LinearLayout.LayoutParams(-1, dp(50)));
        open.setOnClickListener(v -> syncPredictionHistory());
        mainLayout.addView(validationCard);
    }

    private void buildSettingsScreen() {
        addHeading("07", "SETTINGS");
        TextView note = label("Manage data connection, app information and prediction history.");
        note.setTextColor(MUTED);
        note.setTextSize(13);
        note.setPadding(0, 0, 0, dp(12));
        mainLayout.addView(note);

        addSettingsItem("👤  ACCOUNT", "Account and profile options", () ->
            new AlertDialog.Builder(this).setTitle("Account")
                .setMessage("Football Genie account features are not connected in this build.")
                .setPositiveButton("OK", null).show());

        addSettingsItem("🔔  NOTIFICATIONS", "Match alerts and updates", () ->
            new AlertDialog.Builder(this).setTitle("Notifications")
                .setMessage("Notification preferences are not enabled in this build.")
                .setPositiveButton("OK", null).show());

        addSettingsItem("⚙  APP PREFERENCES", "Theme, language and units", () ->
            new AlertDialog.Builder(this).setTitle("App Preferences")
                .setMessage("Football Genie currently uses its dark navy and neon-green theme. Additional preferences can be added without changing the prediction model.")
                .setPositiveButton("OK", null).show());

        addSettingsItem("🔄  DATA & SYNC", "Check the Football Genie API connection", () -> checkApiConnection());

        addSettingsItem("🗑  RESET PREDICTION HISTORY", "Clear all saved predictions after confirmation", () -> confirmResetPredictionHistory());

        addSettingsItem("ℹ  ABOUT", "Version and model information", () ->
            new AlertDialog.Builder(this).setTitle("About Football Genie")
                .setMessage("Football Genie predicts and simulates football outcomes using available statistics and expected-goal estimates. Probabilities are estimates, not guarantees.")
                .setPositiveButton("OK", null).show());
    }

    private void addSettingsItem(String title, String description, Runnable action) {
        LinearLayout item = card();
        TextView heading = label(title);
        heading.setTextColor(GREEN);
        heading.setTextSize(13);
        heading.setTypeface(null, Typeface.BOLD);
        item.addView(heading);
        TextView detail = label(description);
        detail.setTextColor(MUTED);
        detail.setTextSize(12);
        detail.setPadding(0, dp(5), 0, dp(2));
        item.addView(detail);
        item.setClickable(true);
        item.setOnClickListener(v -> action.run());
        mainLayout.addView(item);
    }

    private void buildHeader() {
        LinearLayout brandBar = new LinearLayout(this);
        brandBar.setOrientation(LinearLayout.HORIZONTAL);
        brandBar.setGravity(Gravity.CENTER_VERTICAL);
        brandBar.setPadding(dp(4), dp(8), dp(4), dp(14));

        TextView mark = label("⚽");
        mark.setTextColor(GREEN);
        mark.setTextSize(29);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(Color.rgb(8, 43, 48), 30));
        brandBar.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout brandWords = new LinearLayout(this);
        brandWords.setOrientation(LinearLayout.VERTICAL);
        brandWords.setPadding(dp(10), 0, 0, 0);
        TextView name = label("Football");
        name.setTextColor(WHITE);
        name.setTextSize(24);
        name.setTypeface(null, Typeface.BOLD);
        brandWords.addView(name);
        TextView genie = label("GENIE");
        genie.setTextColor(GREEN);
        genie.setTextSize(24);
        genie.setTypeface(null, Typeface.BOLD);
        brandWords.addView(genie);
        TextView tagline = label("P R E D I C T  •  S I M U L A T E  •  W I N");
        tagline.setTextColor(MUTED);
        tagline.setTextSize(8);
        tagline.setPadding(0, dp(3), 0, 0);
        brandWords.addView(tagline);
        brandBar.addView(brandWords, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView settings = label("⚙");
        settings.setTextColor(WHITE);
        settings.setTextSize(25);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(rounded(CARD, 14));
        brandBar.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));
        settings.setOnClickListener(v -> showScreen(7));
        mainLayout.addView(brandBar);

        TextView slogan = label("SMARTER PREDICTIONS.  DEEPER INSIGHTS.");
        slogan.setTextColor(GREEN);
        slogan.setTextSize(10);
        slogan.setTypeface(null, Typeface.BOLD);
        slogan.setGravity(Gravity.CENTER);
        slogan.setPadding(0, 0, 0, dp(15));
        mainLayout.addView(slogan);
    }

    private void buildMatchForm() {
        addHeading("01", "MATCH SETUP");

        LinearLayout introCard = card();
        introCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView introTag = label("⚽  MATCH INTELLIGENCE");
        introTag.setTextColor(GREEN);
        introTag.setTextSize(11);
        introTag.setTypeface(null, Typeface.BOLD);
        introCard.addView(introTag);

        TextView introTitle = label("Build your match prediction");
        introTitle.setTextColor(WHITE);
        introTitle.setTextSize(21);
        introTitle.setTypeface(null, Typeface.BOLD);
        introTitle.setPadding(0, dp(7), 0, dp(5));
        introCard.addView(introTitle);

        TextView introNote = label("Choose a fixture or enter both teams and their expected goals (xG). Football Genie will model 10,000 possible scorelines.");
        introNote.setTextColor(MUTED);
        introNote.setTextSize(13);
        introNote.setLineSpacing(dp(3), 1.0f);
        introCard.addView(introNote);
        mainLayout.addView(introCard);

        LinearLayout teamsCard = card();
        TextView teamsTitle = label("01  •  SELECT THE TEAMS");
        teamsTitle.setTextColor(GREEN);
        teamsTitle.setTextSize(12);
        teamsTitle.setTypeface(null, Typeface.BOLD);
        teamsTitle.setPadding(0, 0, 0, dp(12));
        teamsCard.addView(teamsTitle);

        TextView homeLabel = label("HOME TEAM");
        homeLabel.setTextColor(MUTED);
        homeLabel.setTextSize(10);
        homeLabel.setTypeface(null, Typeface.BOLD);
        teamsCard.addView(homeLabel);
        homeInput = addInput(teamsCard, "Enter home team", false);
        homeInput.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));

        TextView vs = label("VS");
        vs.setTextColor(GREEN);
        vs.setTextSize(11);
        vs.setTypeface(null, Typeface.BOLD);
        vs.setGravity(Gravity.CENTER);
        vs.setBackground(rounded(Color.rgb(17, 49, 52), 9));
        LinearLayout.LayoutParams vsParams = new LinearLayout.LayoutParams(dp(48), dp(28));
        vsParams.gravity = Gravity.CENTER_HORIZONTAL;
        vsParams.setMargins(0, 0, 0, dp(10));
        teamsCard.addView(vs, vsParams);

        TextView awayLabel = label("AWAY TEAM");
        awayLabel.setTextColor(MUTED);
        awayLabel.setTextSize(10);
        awayLabel.setTypeface(null, Typeface.BOLD);
        teamsCard.addView(awayLabel);
        awayInput = addInput(teamsCard, "Enter away team", false);
        awayInput.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));

        Button fixturesButton = new Button(this);
        fixturesButton.setText("🌐  BROWSE LIVE FIXTURES");
        fixturesButton.setTextColor(WHITE);
        fixturesButton.setTextSize(13);
        fixturesButton.setTypeface(null, Typeface.BOLD);
        fixturesButton.setAllCaps(false);
        fixturesButton.setBackground(rounded(Color.rgb(37, 57, 78), 13));
        LinearLayout.LayoutParams fixturesParams = new LinearLayout.LayoutParams(-1, dp(50));
        fixturesParams.topMargin = dp(4);
        teamsCard.addView(fixturesButton, fixturesParams);
        fixturesButton.setOnClickListener(v -> {
            showScreen(5);
            loadFixtures();
        });
        mainLayout.addView(teamsCard);

        LinearLayout xgCard = card();
        TextView xgTitle = label("02  •  EXPECTED GOALS (xG)");
        xgTitle.setTextColor(GREEN);
        xgTitle.setTextSize(12);
        xgTitle.setTypeface(null, Typeface.BOLD);
        xgTitle.setPadding(0, 0, 0, dp(5));
        xgCard.addView(xgTitle);
        TextView xgNote = label("Estimate each team's scoring strength. Use recent form and available statistics where possible.");
        xgNote.setTextColor(MUTED);
        xgNote.setTextSize(12);
        xgNote.setLineSpacing(dp(2), 1.0f);
        xgNote.setPadding(0, 0, 0, dp(10));
        xgCard.addView(xgNote);

        LinearLayout xgRow = new LinearLayout(this);
        xgRow.setOrientation(LinearLayout.HORIZONTAL);
        xgCard.addView(xgRow, new LinearLayout.LayoutParams(-1, -2));
        homeGoals = addInput(xgRow, "Home xG (e.g. 1.6)", true);
        awayGoals = addInput(xgRow, "Away xG (e.g. 1.1)", true);
        makeCompactInput(homeGoals);
        makeCompactInput(awayGoals);
        mainLayout.addView(xgCard);

        LinearLayout formCard = card();
        TextView formTitle = label("03  •  RECENT TEAM FORM");
        formTitle.setTextColor(GREEN);
        formTitle.setTextSize(12);
        formTitle.setTypeface(null, Typeface.BOLD);
        formTitle.setPadding(0, 0, 0, dp(8));
        formCard.addView(formTitle);

        statsSummary = new LinearLayout(this);
        statsSummary.setOrientation(LinearLayout.VERTICAL);
        statsSummary.setPadding(dp(10), dp(10), dp(10), dp(10));
        statsSummary.setBackground(rounded(Color.rgb(8, 23, 39), 14));
        formCard.addView(statsSummary, new LinearLayout.LayoutParams(-1, -2));
        renderRecentFormMessage("TEAM FORM NOT LOADED",
            "Choose a live fixture to compare both teams' recent results, scoring, and defensive records.");
        mainLayout.addView(formCard);

        LinearLayout runCard = card();
        TextView runNote = label("READY TO ANALYSE?");
        runNote.setTextColor(MUTED);
        runNote.setTextSize(10);
        runNote.setTypeface(null, Typeface.BOLD);
        runNote.setGravity(Gravity.CENTER);
        runNote.setPadding(0, 0, 0, dp(8));
        runCard.addView(runNote);

        Button simulateButton = new Button(this);
        simulateButton.setText("⚽  SIMULATE 10,000 MATCHES");
        simulateButton.setTextColor(Color.rgb(8, 25, 22));
        simulateButton.setTextSize(14);
        simulateButton.setTypeface(null, Typeface.BOLD);
        simulateButton.setAllCaps(false);
        simulateButton.setBackground(rounded(GREEN, 14));
        runCard.addView(simulateButton, new LinearLayout.LayoutParams(-1, dp(56)));
        simulateButton.setOnClickListener(v -> {
            hideKeyboard();
            simulate();
        });
        mainLayout.addView(runCard);
    }

    private void buildHistoryTools() {
        addHeading("03", "PREDICTION HISTORY & TESTING");
        LinearLayout historyCard = card();
        historyCard.setOrientation(LinearLayout.VERTICAL);
        TextView historyNote = label("Review saved predictions, update finished match results, or remove records you no longer need.");
        historyNote.setTextColor(MUTED);
        historyNote.setTextSize(12);
        historyNote.setLineSpacing(dp(3), 1.0f);
        historyNote.setPadding(0, 0, 0, dp(10));
        historyCard.addView(historyNote);
        Button historyButton = new Button(this);
        historyButton.setText("📊  PREDICTION HISTORY / UPDATE RESULTS");
        historyButton.setTextColor(WHITE);
        historyButton.setTextSize(13);
        historyButton.setAllCaps(false);
        historyButton.setBackground(rounded(CARD, 14));
        LinearLayout.LayoutParams historyParams =
        new LinearLayout.LayoutParams(-1, dp(50));
        historyParams.bottomMargin = dp(12);
        historyCard.addView(historyButton, historyParams);
        historyButton.setOnClickListener(v -> syncPredictionHistory());
        
        Button resetHistoryButton = new Button(this);
        resetHistoryButton.setText("RESET PREDICTION HISTORY");
        resetHistoryButton.setTextColor(Color.rgb(255, 235, 235));
        resetHistoryButton.setTextSize(13);
        resetHistoryButton.setAllCaps(false);
        resetHistoryButton.setBackground(rounded(Color.rgb(125, 39, 53), 14));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1, dp(48));
        resetParams.bottomMargin = dp(12);
        historyCard.addView(resetHistoryButton, resetParams);
        resetHistoryButton.setOnClickListener(v -> confirmResetPredictionHistory());
        
        Button removeFixtureButton = new Button(this);
        removeFixtureButton.setText("REMOVE ONE FIXTURE FROM HISTORY");
        removeFixtureButton.setTextColor(Color.WHITE);
        removeFixtureButton.setTextSize(13);
        removeFixtureButton.setAllCaps(false);
        removeFixtureButton.setBackground(rounded(Color.rgb(48, 75, 103), 14));
        LinearLayout.LayoutParams removeFixtureParams = new LinearLayout.LayoutParams(-1, dp(48));
        removeFixtureParams.bottomMargin = dp(12);
        historyCard.addView(removeFixtureButton, removeFixtureParams);
        removeFixtureButton.setOnClickListener(v -> choosePredictionToRemove());
        mainLayout.addView(historyCard);
    }

    private void buildResults() {
        addHeading("02", "MATCH PREDICTION");

        LinearLayout outputCard = card();
        outputCard.setOrientation(LinearLayout.VERTICAL);

        status = label("AWAITING MATCH SIMULATION");
        status.setTextColor(GREEN);
        status.setTextSize(12);
        status.setTypeface(null, Typeface.BOLD);
        status.setPadding(dp(10), dp(10), dp(10), dp(10));
        status.setBackground(rounded(Color.rgb(17, 49, 52), 10));
        outputCard.addView(status);

        TextView outcomeHeading = label("1X2 WIN PROBABILITIES");
        outcomeHeading.setTextColor(MUTED);
        outcomeHeading.setTextSize(10);
        outcomeHeading.setTypeface(null, Typeface.BOLD);
        outcomeHeading.setPadding(0, dp(16), 0, dp(8));
        outputCard.addView(outcomeHeading);

        LinearLayout outcomeRow = new LinearLayout(this);
        outcomeRow.setOrientation(LinearLayout.HORIZONTAL);
        homeWinValue = addMetricCard(outcomeRow, "HOME WIN", "--", GREEN);
        drawValue = addMetricCard(outcomeRow, "DRAW", "--", WHITE);
        awayWinValue = addMetricCard(outcomeRow, "AWAY WIN", "--", GREEN);
        outputCard.addView(outcomeRow);

        TextView goalsHeading = label("GOALS MARKETS");
        goalsHeading.setTextColor(MUTED);
        goalsHeading.setTextSize(10);
        goalsHeading.setTypeface(null, Typeface.BOLD);
        goalsHeading.setPadding(0, dp(16), 0, dp(8));
        outputCard.addView(goalsHeading);

        LinearLayout goalsRow = new LinearLayout(this);
        goalsRow.setOrientation(LinearLayout.HORIZONTAL);
        overValue = addMetricCard(goalsRow, "OVER 2.5", "--", GREEN);
        underValue = addMetricCard(goalsRow, "UNDER 2.5", "--", WHITE);
        bttsValue = addMetricCard(goalsRow, "BTTS YES", "--", GREEN);
        outputCard.addView(goalsRow);

        TextView detailsHeading = label("MATCH INTELLIGENCE & MODEL ANALYSIS");
        detailsHeading.setTextColor(GREEN);
        detailsHeading.setTextSize(11);
        detailsHeading.setTypeface(null, Typeface.BOLD);
        detailsHeading.setPadding(0, dp(18), 0, dp(4));
        outputCard.addView(detailsHeading);

        TextView analysisLabel = label("DETAILED MATCH INTELLIGENCE");
        analysisLabel.setTextColor(GREEN);
        analysisLabel.setTextSize(13);
        analysisLabel.setTypeface(null, Typeface.BOLD);
        analysisLabel.setPadding(0, dp(18), 0, dp(8));
        outputCard.addView(analysisLabel);

        result = label("Run a match simulation to generate the prediction summary, scoreline outlook, goal probabilities and match-flow analysis.");
        result.setTextColor(WHITE);
        result.setTextSize(14);
        result.setLineSpacing(dp(6), 1.08f);
        result.setPadding(dp(14), dp(16), dp(14), dp(16));
        result.setBackground(rounded(Color.rgb(15, 26, 43), 14));
        result.setGravity(Gravity.START);
        outputCard.addView(result);
        mainLayout.addView(outputCard);

        TextView footer = label("FOOTBALL GENIE  •  STATISTICS, NOT CERTAINTY\nProbabilities describe modelled outcomes. Real matches can differ.");
        footer.setTextColor(MUTED);
        footer.setTextSize(10);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, dp(10));
        mainLayout.addView(footer);
    }

    private TextView addMetricCard(LinearLayout row, String heading, String initial, int valueColor) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setGravity(Gravity.CENTER);
        metric.setPadding(dp(3), dp(12), dp(3), dp(12));
        metric.setBackground(rounded(Color.rgb(26, 43, 61), 13));
        metric.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(76), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(metric, params);

        TextView title = label(heading);
        title.setTextColor(MUTED);
        title.setTextSize(8);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        metric.addView(title);

        TextView value = label(initial);
        value.setTextColor(valueColor);
        value.setTextSize(18);
        value.setTypeface(null, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, dp(5), 0, 0);
        metric.addView(value);
        return value;
    }

    private void addHeading(String number, String heading) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(dp(2), dp(24), 0, dp(12));

        TextView numberView = label(number);
        numberView.setTextColor(Color.rgb(8, 25, 22));
        numberView.setTextSize(11);
        numberView.setTypeface(null, Typeface.BOLD);
        numberView.setGravity(Gravity.CENTER);
        numberView.setBackground(rounded(GREEN, 9));
        line.addView(numberView, new LinearLayout.LayoutParams(dp(34), dp(30)));

        TextView titleView = label(heading);
        titleView.setTextColor(WHITE);
        titleView.setTextSize(15);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(dp(10), 0, 0, 0);
        line.addView(titleView);
        mainLayout.addView(line);
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(rounded(CARD, 18));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        layout.setLayoutParams(params);
        return layout;
    }

    

    private void makeCompactInput(EditText input) {
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(0, dp(52), 1f);
        params.setMargins(dp(4), 0, dp(4), dp(12));
        input.setLayoutParams(params);
    }

    private EditText addInput(
            LinearLayout parent,
            String hint,
            boolean numeric) {

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(MUTED);
        input.setTextColor(WHITE);
        input.setTextSize(14);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(rounded(
            Color.rgb(31, 44, 65), 10
        ));

        if (numeric) {
            input.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
        } else {
            input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_WORDS
            );
        }

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, dp(52));

        params.bottomMargin = dp(12);
        parent.addView(input, params);

        return input;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(int value) {
        return (int) (
            value * getResources()
                .getDisplayMetrics().density + 0.5f
        );
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager =
                (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

            manager.hideSoftInputFromWindow(
                homeInput.getWindowToken(), 0
            );
        } catch (Exception ignored) {
        }
    }

    private double poisson(Random random, double lambda) {
        double limit = Math.exp(-lambda);
        double product = 1.0;
        int count = 0;

        do {
            count++;
            product *= random.nextDouble();
        } while (product > limit);

        return count - 1;
    }

    private double poissonProbability(int goals, double lambda) {
        double probability = Math.exp(-lambda);
        for (int i = 1; i <= goals; i++) {
            probability *= lambda / i;
        }
        return probability;
    }

    // Dixon-Coles low-score correction: football draws and one-goal results
    // can differ from a fully independent Poisson model.
    private double lowScoreCorrection(int homeGoals, int awayGoals,
            double homeXg, double awayXg) {
        final double rho = -0.08;
        if (homeGoals == 0 && awayGoals == 0) {
            return 1.0 - homeXg * awayXg * rho;
        }
        if (homeGoals == 0 && awayGoals == 1) {
            return 1.0 + homeXg * rho;
        }
        if (homeGoals == 1 && awayGoals == 0) {
            return 1.0 + awayXg * rho;
        }
        if (homeGoals == 1 && awayGoals == 1) {
            return 1.0 - rho;
        }
        return 1.0;
    }

    // Hypothetical match-flow estimate from a score and minute.
    // Remaining xG is scaled by time left; score-state effects are illustrative assumptions.
    private String scenarioOutcomeSummary(String home, String away,
            double homeXg, double awayXg, int minute, int currentHomeGoals,
            int currentAwayGoals, double homeAttackFactor, double awayAttackFactor) {
        double timeLeft = Math.max(0.0, Math.min(1.0, (90.0 - minute) / 90.0));
        double remainingHomeXg = homeXg * timeLeft * homeAttackFactor;
        double remainingAwayXg = awayXg * timeLeft * awayAttackFactor;
        double homeWin = 0.0, draw = 0.0, awayWin = 0.0, total = 0.0;
        for (int h = 0; h <= 8; h++) {
            for (int a = 0; a <= 8; a++) {
                double p = poissonProbability(h, remainingHomeXg)
                    * poissonProbability(a, remainingAwayXg);
                total += p;
                int finalHome = currentHomeGoals + h;
                int finalAway = currentAwayGoals + a;
                if (finalHome > finalAway) homeWin += p;
                else if (finalHome == finalAway) draw += p;
                else awayWin += p;
            }
        }
        if (total > 0.0) {
            homeWin /= total;
            draw /= total;
            awayWin /= total;
        }
        return String.format(Locale.US,
            "%s %.1f%% | Draw %.1f%% | %s %.1f%%",
            home, homeWin * 100.0, draw * 100.0, away, awayWin * 100.0);
    }

    private void simulate() {
        try {
            String home =
                homeInput.getText().toString().trim();

            String away =
                awayInput.getText().toString().trim();

            double homeXg = Double.parseDouble(
                homeGoals.getText().toString().trim()
                    .replace(',', '.')
            );

            double awayXg = Double.parseDouble(
                awayGoals.getText().toString().trim()
                    .replace(',', '.')
            );

            if (home.isEmpty() || away.isEmpty()
                    || homeXg < 0 || awayXg < 0
                    || homeXg > 8 || awayXg > 8) {
                throw new IllegalArgumentException();
            }

            int homeWins = 0;
            int draws = 0;
            int awayWins = 0;
            int over25 = 0;
            int btts = 0;

            // Size the scoreline grid from the entered xG values plus a generous tail margin.
            // Outcome and goals-market counts include every simulation; this grid ranks exact scores.
            int scoreGridSize = Math.max(13,
                (int) Math.ceil(Math.max(homeXg, awayXg) + 10.0));
            int[][] scores = new int[scoreGridSize][scoreGridSize];

            Random random = new Random();

            for (int i = 0; i < 10000; i++) {
                int hg = (int) poisson(random, homeXg);
                int ag = (int) poisson(random, awayXg);

                if (hg > ag) {
                    homeWins++;
                } else if (hg == ag) {
                    draws++;
                } else {
                    awayWins++;
                }

                if (hg + ag > 2) {
                    over25++;
                }

                if (hg > 0 && ag > 0) {
                    btts++;
                }

                if (hg < scores.length && ag < scores[0].length) {
                    scores[hg][ag]++;
                }
            }

            int bestHome = 0;
            int bestAway = 0;
            int bestCount = 0;

            for (int h = 0; h < scores.length; h++) {
                for (int a = 0; a < scores[h].length; a++) {
                    if (scores[h][a] > bestCount) {
                        bestCount = scores[h][a];
                        bestHome = h;
                        bestAway = a;
                    }
                }
            }

            // Rank exact scorelines by their theoretical independent-Poisson probability.
            // This avoids ranking noise caused by random variation in the 10,000 simulations.
            double[][] rankedScoreProbabilities =
                new double[scores.length][scores[0].length];
            double rankedScoreProbabilityTotal = 0.0;
            for (int h = 0; h < rankedScoreProbabilities.length; h++) {
                for (int a = 0; a < rankedScoreProbabilities[h].length; a++) {
                    rankedScoreProbabilities[h][a] =
                        poissonProbability(h, homeXg) * poissonProbability(a, awayXg)
                        * lowScoreCorrection(h, a, homeXg, awayXg);
                    rankedScoreProbabilityTotal += rankedScoreProbabilities[h][a];
                }
            }
            StringBuilder topScores = new StringBuilder();
            for (int rank = 0; rank < 3; rank++) {
                int rankHome = -1;
                int rankAway = -1;
                double rankProbability = 0.0;
                for (int h = 0; h < rankedScoreProbabilities.length; h++) {
                    for (int a = 0; a < rankedScoreProbabilities[h].length; a++) {
                        if (rankedScoreProbabilities[h][a] > rankProbability) {
                            rankProbability = rankedScoreProbabilities[h][a];
                            rankHome = h;
                            rankAway = a;
                        }
                    }
                }
                if (rankHome < 0) break;
                if (topScores.length() > 0) topScores.append("\n");
                topScores.append(rank + 1).append(". ")
                    .append(rankHome).append("-").append(rankAway)
                    .append("  (").append(String.format(Locale.US, "%.1f",
                        rankedScoreProbabilityTotal > 0.0
                            ? rankProbability * 100.0 / rankedScoreProbabilityTotal
                            : rankProbability * 100.0)).append("%)");
                rankedScoreProbabilities[rankHome][rankAway] = 0.0;
            }

            // Calculate market probabilities from the Poisson score distribution.
            // This removes Monte Carlo sampling noise from displayed probabilities.
            double homeProbability = 0.0;
            double drawProbability = 0.0;
            double awayProbability = 0.0;
            double overProbability = 0.0;
            double bttsProbability = 0.0;
            for (int h = 0; h < scores.length; h++) {
                double homeGoalProbability = poissonProbability(h, homeXg);
                for (int a = 0; a < scores[h].length; a++) {
                    double jointProbability = homeGoalProbability
                        * poissonProbability(a, awayXg)
                        * lowScoreCorrection(h, a, homeXg, awayXg);
                    if (h > a) homeProbability += jointProbability;
                    else if (h == a) drawProbability += jointProbability;
                    else awayProbability += jointProbability;
                    if (h + a > 2) overProbability += jointProbability;
                    if (h > 0 && a > 0) bttsProbability += jointProbability;
                }
            }
            double probabilityTotal = homeProbability + drawProbability + awayProbability;
            if (probabilityTotal > 0.0) {
                homeProbability /= probabilityTotal;
                drawProbability /= probabilityTotal;
                awayProbability /= probabilityTotal;
                overProbability /= probabilityTotal;
                bttsProbability /= probabilityTotal;
            }
            double homeP = homeProbability * 100.0;
            double drawP = drawProbability * 100.0;
            double awayP = awayProbability * 100.0;
            double overP = overProbability * 100.0;
            double underP = 100.0 - overP;
            double bttsP = bttsProbability * 100.0;

            String outcome;
            double highest;

            if (homeP >= drawP && homeP >= awayP) {
                outcome = home + " win";
                highest = homeP;
            } else if (drawP >= homeP && drawP >= awayP) {
                outcome = "Draw";
                highest = drawP;
            } else {
                outcome = away + " win";
                highest = awayP;
            }

            double secondHighest = Math.max(
                Math.min(homeP, drawP),
                Math.max(Math.min(homeP, awayP), Math.min(drawP, awayP))
            );
            double probabilityGap = highest - secondHighest;
            String confidence;
            String confidenceReason;
            if (highest >= 60.0 && probabilityGap >= 15.0) {
                confidence = "STRONGER MODEL LEAN";
                confidenceReason = "The leading outcome has a clearer probability advantage, but an upset remains possible.";
            } else if (highest >= 45.0 && probabilityGap >= 8.0) {
                confidence = "MODERATE MODEL LEAN";
                confidenceReason = "One outcome leads, although the alternatives remain realistic.";
            } else {
                confidence = "LOW CONFIDENCE / CLOSE MATCH";
                confidenceReason = "The outcome probabilities are relatively close; avoid treating one pick as a safe result.";
            }

            double totalExpectedGoals = homeXg + awayXg;
            // Approximate first-half scoring using 45% of each team's match xG.
            // First-to-score uses proportional scoring rates and is not a live event forecast.
            double firstHalfGoalProbability = 1.0
                - Math.exp(-0.45 * totalExpectedGoals);
            double firstHalfHomeLead = 0.0;
            double firstHalfDraw = 0.0;
            double firstHalfAwayLead = 0.0;
            double firstHalfOutcomeTotal = 0.0;
            for (int halfHome = 0; halfHome <= 8; halfHome++) {
                for (int halfAway = 0; halfAway <= 8; halfAway++) {
                    double halfProbability =
                        poissonProbability(halfHome, homeXg * 0.45)
                        * poissonProbability(halfAway, awayXg * 0.45);
                    firstHalfOutcomeTotal += halfProbability;
                    if (halfHome > halfAway) firstHalfHomeLead += halfProbability;
                    else if (halfHome == halfAway) firstHalfDraw += halfProbability;
                    else firstHalfAwayLead += halfProbability;
                }
            }
            if (firstHalfOutcomeTotal > 0.0) {
                firstHalfHomeLead /= firstHalfOutcomeTotal;
                firstHalfDraw /= firstHalfOutcomeTotal;
                firstHalfAwayLead /= firstHalfOutcomeTotal;
            }
            double secondHalfGoalProbability = 1.0
                - Math.exp(-0.55 * totalExpectedGoals);
            double noGoalProbability = (
                poissonProbability(0, homeXg) * poissonProbability(0, awayXg)
                * lowScoreCorrection(0, 0, homeXg, awayXg)
            ) / Math.max(0.000001, rankedScoreProbabilityTotal);
            noGoalProbability = Math.max(0.0, Math.min(1.0, noGoalProbability));
            double anyGoalProbability = 1.0 - noGoalProbability;
            double homeFirstScoreProbability = totalExpectedGoals > 0
                ? (homeXg / totalExpectedGoals) * anyGoalProbability : 0.0;
            double awayFirstScoreProbability = totalExpectedGoals > 0
                ? (awayXg / totalExpectedGoals) * anyGoalProbability : 0.0;
            String tempoProfile;
            if (totalExpectedGoals < 1.8) {
                tempoProfile = "LOW-SCORING PROFILE";
            } else if (totalExpectedGoals < 2.6) {
                tempoProfile = "MODERATE SCORING PROFILE";
            } else if (totalExpectedGoals < 3.4) {
                tempoProfile = "OPEN MATCH PROFILE";
            } else {
                tempoProfile = "HIGH-SCORING PROFILE";
            }
            double xgDifference = Math.abs(homeXg - awayXg);
            String balanceProfile = xgDifference < 0.25
                ? "Very balanced on expected goals."
                : (xgDifference < 0.70
                    ? "One team has a modest expected-goals edge."
                    : "The expected-goals inputs show a clear attacking edge.");

            double underdogWinProbability = homeXg >= awayXg ? awayP : homeP;
            String upsetAssessment;
            if (underdogWinProbability >= 30.0) {
                upsetAssessment = "ELEVATED UPSET POSSIBILITY: the team with the lower xG still has a meaningful win probability.";
            } else if (underdogWinProbability >= 20.0) {
                upsetAssessment = "UPSET WATCH: the team with the lower xG retains a plausible win route.";
            } else {
                upsetAssessment = "No strong upset signal from the xG inputs alone; surprises remain possible.";
            }

            String goalTrend = overP >= 50
                ? "Over 2.5 goals is more likely than Under 2.5."
                : "Under 2.5 goals is more likely than Over 2.5.";

            String bttsTrend = bttsP >= 50
                ? "Both teams scoring is more likely than not."
                : "At least one team failing to score is more likely.";

            String levelAt60 = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 0, 1.0, 1.0);
            String homeLeadsAt60 = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 1, 0, 0.85, 1.20);
            String awayLeadsAt60 = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 1, 1.20, 0.85);

            String homeTrailsAt60 = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 1, 1.20, 0.85);
            String awayTrailsAt60 = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 1, 0, 0.85, 1.20);

            double homeSecondHalfGoalChance = 1.0 - Math.exp(-homeXg * 0.55);
            double awaySecondHalfGoalChance = 1.0 - Math.exp(-awayXg * 0.55);

            String homeRedCardScenario = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 0, 0.65, 1.20);
            String awayRedCardScenario = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 0, 1.20, 0.65);

            String homeAggressiveScenario = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 0, 1.25, 1.10);
            String awayAggressiveScenario = scenarioOutcomeSummary(home, away, homeXg, awayXg,
                60, 0, 0, 1.10, 1.25);

            String analysis =
                "\n\n━━━━━━━━━━━━━━━━━━\n" +
                "WHY THIS PREDICTION?\n\n" +
                "Expected goals\n" +
                home + " entered: " +
                String.format(Locale.US, "%.2f", homeXg) +
                "\n" + away + " entered: " +
                String.format(Locale.US, "%.2f", awayXg) +
                "\n\n";

            if (homeXg > awayXg) {
                analysis += home +
                    " has the higher expected-goals input. " +
                    "That gives the model a higher expected " +
                    "scoring rate for the home team.\n\n";
            } else if (awayXg > homeXg) {
                analysis += away +
                    " has the higher expected-goals input. " +
                    "That gives the model a higher expected " +
                    "scoring rate for the away team.\n\n";
            } else {
                analysis += "Both teams have equal expected-goals " +
                    "inputs, so this input alone does not favour " +
                    "either team.\n\n";
            }

            analysis +=
                "Most likely match outcome\n" +
                outcome + " is the most likely of the three " +
                "outcomes in this simulation (" +
                String.format(Locale.US, "%.1f", highest) +
                "%). This is not a guarantee.\n\n" +

                "Confidence assessment\n" + confidence + "\n" +
                confidenceReason + "\n" +
                "This rating reflects simulation probabilities only; it is not a guarantee of accuracy.\n\n" +
                "Upset watch\n" + upsetAssessment + "\n\n" +

                "Match tempo and balance\n" + tempoProfile +
                " (combined expected goals: " +
                String.format(Locale.US, "%.2f", totalExpectedGoals) + "). " +
                balanceProfile + "\n\n" +
                "Goals analysis\n" + goalTrend +
                "\n" + bttsTrend +
                "\n\n" +

                "First-half and first-goal outlook\n" +
                "At least one first-half goal: " +
                String.format(Locale.US, "%.1f", firstHalfGoalProbability * 100.0) + "%\n" +
                "Approximate half-time: " + home + " leading " +
                String.format(Locale.US, "%.1f", firstHalfHomeLead * 100.0) + "%, draw " +
                String.format(Locale.US, "%.1f", firstHalfDraw * 100.0) + "%, " +
                away + " leading " +
                String.format(Locale.US, "%.1f", firstHalfAwayLead * 100.0) + "%\n" +
                "At least one second-half goal: " +
                String.format(Locale.US, "%.1f", secondHalfGoalProbability * 100.0) + "%\n" +
                home + " to score first: " +
                String.format(Locale.US, "%.1f", homeFirstScoreProbability * 100.0) + "%\n" +
                away + " to score first: " +
                String.format(Locale.US, "%.1f", awayFirstScoreProbability * 100.0) + "%\n" +
                "No goals in the match: " +
                String.format(Locale.US, "%.1f", noGoalProbability * 100.0) + "%\n\n" +
                "Most frequent score\n" +
                home + " " + bestHome + " - " +
                bestAway + " " + away +
                " was the most frequent exact score in " +
                "these simulation runs.\n\n" +

                "Hypothetical match flow at 60 minutes\n" +
                "If level at 0-0: " + levelAt60 + "\n" +
                "If " + home + " leads 1-0: " + homeLeadsAt60 + "\n" +
                "If " + away + " leads 1-0: " + awayLeadsAt60 + "\n" +
                "These are conditional model scenarios, not live match readings.\n\n" +

                "Comeback outlook if trailing 0-1 at 60: " + homeTrailsAt60 + "\n" +
                "Comeback outlook if trailing 1-0 at 60: " + awayTrailsAt60 + "\n\n" +
                "Second-half scoring outlook (time-scaled xG): " + home + " to score " +
                String.format(Locale.US, "%.1f", homeSecondHalfGoalChance * 100.0) + "%; " +
                away + " to score " + String.format(Locale.US, "%.1f", awaySecondHalfGoalChance * 100.0) + "%\\n\\n" +
                "Hypothetical red card at 60 minutes, score 0-0:\\n" +
                "Red card for " + home + ": " + homeRedCardScenario + "\\n" +
                "Red card for " + away + ": " + awayRedCardScenario + "\\n" +
                "Illustrative effect only; red-card impact is not calibrated from event data.\\n\\n" +
                "If " + home + " attacks more aggressively from 60 minutes: " + homeAggressiveScenario + "\\n" +
                "If " + away + " attacks more aggressively from 60 minutes: " + awayAggressiveScenario + "\\n" +
                "Urgency multipliers are assumptions, not observed tactical behaviour.\\n\\n" +
                "SCENARIO COMPARISON\\n" +
                "Compare level, leading, trailing, red-card and aggressive-approach scenarios above. " +
                "A changed probability reflects changed assumptions and score state, not certainty.\\n\\n" +
                "MATCH-FLOW SUMMARY\\n" +
                "This panel explores conditional outcomes at the 60-minute mark, second-half scoring " +
                "and illustrative red-card or urgency scenarios. It does not track a live match or " +
                "predict exact tactical reactions. Use confirmed live score, minute and event data " +
                "before interpreting a real in-play situation.\\n\\n" +
                "IMPORTANT LIMITATIONS\n" +
                "Expected goals can be estimated from recent completed matches " +
                "when you select a fixture, or entered manually. This model does " +
                "not currently account for injuries, confirmed line-ups, weather " +
                "or every relevant team statistic. Those factors may change the " +
                "real match outcome.";

            status.setText("✓  SIMULATION COMPLETE");

            homeWinValue.setText(String.format(Locale.US, "%.1f%%", homeP));
            drawValue.setText(String.format(Locale.US, "%.1f%%", drawP));
            awayWinValue.setText(String.format(Locale.US, "%.1f%%", awayP));
            overValue.setText(String.format(Locale.US, "%.1f%%", overP));
            underValue.setText(String.format(Locale.US, "%.1f%%", underP));
            bttsValue.setText(String.format(Locale.US, "%.1f%%", bttsP));

            result.setText(String.format(Locale.US,
                "10,000 SIMULATIONS COMPLETED\n\n" +
                "MODEL LEAN\n%s\n\n" +
                "MOST FREQUENT SCORE\n%s %d - %d %s\n\n" +
                "TOP 3 MOST LIKELY SCORELINES\n%s\n\n" +
                "DETAILED MATCH INTELLIGENCE\n%s",
                confidence,
                home, bestHome, bestAway, away,
                topScores.toString(),
                analysis
            ));

            savePredictionIfEligible(home, away, homeXg, awayXg,
                homeP, drawP, awayP, overP, bttsP, bestHome, bestAway);
            showScreen(2);

        } catch (Exception e) {
            Toast.makeText(
                this,
                "Enter both team names and expected goals from 0 to 8.",
                Toast.LENGTH_LONG
            ).show();
        }
    }
        private void checkApiConnection() {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                    "https://football-genie-api.onrender.com/"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(
                            MainActivity.this,
                            "Football Genie server connected!",
                            Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                            MainActivity.this,
                            "Server response: " + responseCode,
                            Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "Could not connect to server. Check internet.",
                    Toast.LENGTH_LONG
                ).show());

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
                }
    
    private SharedPreferences predictionPrefs() {
        return getSharedPreferences("football_genie_predictions", MODE_PRIVATE);
    }

    private void confirmResetPredictionHistory() {
        new AlertDialog.Builder(this)
            .setTitle("Reset prediction history?")
            .setMessage("This permanently deletes saved predictions and their evaluation results on this phone. It does not change the prediction engine. Continue only if you want a clean testing period.")
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setPositiveButton("Reset history", (dialog, which) -> {
                predictionPrefs().edit().putString("history", "[]").apply();
                Toast.makeText(this, "Prediction history cleared. Ready for new testing.", Toast.LENGTH_LONG).show();
            })
            .show();
    }


    private void choosePredictionToRemove() {
        JSONArray history = readPredictionHistory();
        if (history.length() == 0) {
            new AlertDialog.Builder(this)
                .setTitle("Prediction history is empty")
                .setMessage("Save a prediction for an upcoming fixture first.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        ArrayList<JSONObject> records = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < history.length(); i++) {
            JSONObject record = history.optJSONObject(i);
            if (record == null) continue;
            records.add(record);
            String label = record.optString("home", "Home") + " vs "
                + record.optString("away", "Away");
            String kickoff = record.optString("kickoff", "");
            if (kickoff.length() >= 16) {
                label += " • " + kickoff.substring(0, 10) + " "
                    + kickoff.substring(11, 16) + " UTC";
            }
            labels.add(label);
        }
        if (records.isEmpty()) return;

        new AlertDialog.Builder(this)
            .setTitle("Choose fixture to remove")
            .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                JSONObject selected = records.get(which);
                String home = selected.optString("home", "Home");
                String away = selected.optString("away", "Away");
                new AlertDialog.Builder(this)
                    .setTitle("Remove this prediction?")
                    .setMessage("Remove " + home + " vs " + away
                        + " and its saved evaluation data from this phone? Other fixtures will remain.")
                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                    .setPositiveButton("Remove fixture", (d, w) -> {
                        String targetId = selected.optString("fixtureId", "");
                        JSONArray current = readPredictionHistory();
                        JSONArray updated = new JSONArray();
                        for (int i = 0; i < current.length(); i++) {
                            JSONObject item = current.optJSONObject(i);
                            if (item == null) continue;
                            boolean sameId = !targetId.isEmpty()
                                && targetId.equals(item.optString("fixtureId", ""));
                            boolean sameFallback = targetId.isEmpty()
                                && home.equals(item.optString("home", ""))
                                && away.equals(item.optString("away", ""))
                                && selected.optLong("savedAt", -1L) == item.optLong("savedAt", -2L);
                            if (!sameId && !sameFallback) updated.put(item);
                        }
                        predictionPrefs().edit().putString("history", updated.toString()).apply();
                        Toast.makeText(this, "Fixture removed. Other history entries were kept.",
                            Toast.LENGTH_LONG).show();
                    })
                    .show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private JSONArray readPredictionHistory() {
        try {
            return new JSONArray(predictionPrefs().getString("history", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void savePredictionIfEligible(String home, String away,
            double homeXg, double awayXg, double homeP, double drawP,
            double awayP, double overP, double bttsP, int predictedHome,
            int predictedAway) {
        if (selectedFixture == null) {
            return;
        }

        String fixtureStatus = selectedFixture.optString("status", "");
        if (!"SCHEDULED".equals(fixtureStatus) && !"TIMED".equals(fixtureStatus)) {
            Toast.makeText(this,
                "History saves predictions for upcoming fixtures only.",
                Toast.LENGTH_LONG).show();
            return;
        }

        String fixtureId = selectedFixture.optString("id", "");
        String kickoff = selectedFixture.optString("utcDate", "");
        if (fixtureId.isEmpty()) return;

        try {
            JSONArray oldHistory = readPredictionHistory();
            JSONArray updated = new JSONArray();
            JSONObject record = new JSONObject();
            record.put("fixtureId", fixtureId);
            record.put("home", home);
            record.put("away", away);
            record.put("kickoff", kickoff);
            record.put("homeXg", homeXg);
            record.put("awayXg", awayXg);
            record.put("homeP", homeP);
            record.put("drawP", drawP);
            record.put("awayP", awayP);
            record.put("overP", overP);
            record.put("bttsP", bttsP);
            record.put("predictedHome", predictedHome);
            record.put("predictedAway", predictedAway);
            record.put("predictionStatus", "PENDING");
            record.put("savedAt", System.currentTimeMillis());

            for (int i = 0; i < oldHistory.length(); i++) {
                JSONObject old = oldHistory.optJSONObject(i);
                if (old == null) continue;
                if (!fixtureId.equals(old.optString("fixtureId", ""))) {
                    updated.put(old);
                }
            }
            updated.put(record);
            predictionPrefs().edit().putString("history", updated.toString()).apply();
            Toast.makeText(this,
                "Prediction saved to your on-phone history.",
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                "Could not save this prediction.",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void syncPredictionHistory() {
        Toast.makeText(this, "Checking saved predictions for final results...",
            Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://football-genie-api.onrender.com/api/matches");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();

                JSONObject data = new JSONObject(body.toString());
                JSONArray matches = data.optJSONArray("matches");
                JSONArray history = readPredictionHistory();
                int updatedCount = 0;

                if (matches != null) {
                    for (int i = 0; i < history.length(); i++) {
                        JSONObject record = history.optJSONObject(i);
                        if (record == null ||
                                "FINISHED".equals(record.optString("predictionStatus"))) {
                            continue;
                        }
                        String wantedId = record.optString("fixtureId", "");
                        for (int j = 0; j < matches.length(); j++) {
                            JSONObject match = matches.optJSONObject(j);
                            if (match == null ||
                                !wantedId.equals(match.optString("id", ""))) continue;

                            String matchStatus = match.optString("status", "");
                            record.put("currentMatchStatus", matchStatus);
                            if ("FINISHED".equals(matchStatus)) {
                                JSONObject score = match.optJSONObject("score");
                                JSONObject fullTime = score != null
                                    ? score.optJSONObject("fullTime") : null;
                                if (fullTime != null && !fullTime.isNull("home")
                                        && !fullTime.isNull("away")) {
                                    int actualHome = fullTime.optInt("home", -1);
                                    int actualAway = fullTime.optInt("away", -1);
                                    if (actualHome >= 0 && actualAway >= 0) {
                                        record.put("actualHome", actualHome);
                                        record.put("actualAway", actualAway);
                                        record.put("predictionStatus", "FINISHED");
                                        updatedCount++;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }

                predictionPrefs().edit().putString("history", history.toString()).apply();
                final int updated = updatedCount;
                runOnUiThread(() -> showPredictionHistory(updated));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Could not update results. Please try again when connected.",
                    Toast.LENGTH_LONG).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private double square(double value) {
        return value * value;
    }

    private String calibrationText(int count, int correct) {
        if (count == 0) return "no matches";
        return String.format(Locale.US, "%d matches, %.1f%% correct", count, 100.0 * correct / count);
    }

    private void showPredictionHistory(int newlyUpdated) {
        JSONArray history = readPredictionHistory();
        int finished = 0, outcomeCorrect = 0, goalsCorrect = 0;
        int bttsCorrect = 0, exactCorrect = 0;
        int actualHomeWins = 0, actualDraws = 0, actualAwayWins = 0;
        int invalidProbabilityRecords = 0;
        int pendingCount = 0;
        double outcomeBrierTotal = 0.0;
        double goalsBrierTotal = 0.0;
        double bttsBrierTotal = 0.0;
        double homeGoalAbsError = 0.0, awayGoalAbsError = 0.0;
        double totalGoalAbsError = 0.0;
        int[] confidenceBinCount = new int[5];
        int[] confidenceBinCorrect = new int[5];
        StringBuilder summary = new StringBuilder();
        summary.append("PREDICTION HISTORY\n");
        summary.append("VALIDATION REPORT • v1\\n");
        summary.append("New final results added: ").append(newlyUpdated).append("\n");
        summary.append("Saved records: ").append(history.length()).append(" | evaluated: ").append(finished)
            .append(" | awaiting results: ").append(pendingCount).append("\n\n");

        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject record = history.optJSONObject(i);
            if (record == null) continue;
            String home = record.optString("home", "Home");
            String away = record.optString("away", "Away");
            String state = record.optString("predictionStatus", "PENDING");
            if (!"FINISHED".equals(state)) pendingCount++;
            summary.append(home).append(" vs ").append(away).append("\n");
            summary.append("Expected goals: ")
                .append(String.format(Locale.US, "%.2f", record.optDouble("homeXg", 0)))
                .append(" - ")
                .append(String.format(Locale.US, "%.2f", record.optDouble("awayXg", 0)))
                .append("\n");
            summary.append("Win probabilities (Home / Draw / Away): ")
                .append(String.format(Locale.US, "%.1f%% / %.1f%% / %.1f%%",
                    record.optDouble("homeP", 0),
                    record.optDouble("drawP", 0),
                    record.optDouble("awayP", 0)))
                .append("\n");
            summary.append("Goals market: Over 2.5 ")
                .append(String.format(Locale.US, "%.1f%%", record.optDouble("overP", 0)))
                .append(" | BTTS ")
                .append(String.format(Locale.US, "%.1f%%", record.optDouble("bttsP", 0)))
                .append("\n");
            summary.append("Predicted correct score: ")
                .append(record.optInt("predictedHome", 0))
                .append(" - ")
                .append(record.optInt("predictedAway", 0))
                .append("\n");

            double storedHomeP = record.optDouble("homeP", -1);
            double storedDrawP = record.optDouble("drawP", -1);
            double storedAwayP = record.optDouble("awayP", -1);
            if (storedHomeP < 0 || storedDrawP < 0 || storedAwayP < 0
                    || storedHomeP > 100 || storedDrawP > 100 || storedAwayP > 100
                    || Math.abs(storedHomeP + storedDrawP + storedAwayP - 100.0) > 2.0) {
                invalidProbabilityRecords++;
            }

            if ("FINISHED".equals(state)) {
                int ah = record.optInt("actualHome", -1);
                int aa = record.optInt("actualAway", -1);
                if (ah >= 0 && aa >= 0) {
                    finished++;
                    homeGoalAbsError += Math.abs(record.optInt("predictedHome", 0) - ah);
                    awayGoalAbsError += Math.abs(record.optInt("predictedAway", 0) - aa);
                    totalGoalAbsError += Math.abs(record.optInt("predictedHome", 0) + record.optInt("predictedAway", 0) - ah - aa);
                    String predictedOutcome = record.optDouble("homeP", 0) >=
                        Math.max(record.optDouble("drawP", 0), record.optDouble("awayP", 0))
                        ? "HOME" : (record.optDouble("drawP", 0) >= record.optDouble("awayP", 0)
                            ? "DRAW" : "AWAY");
                    boolean actualHome = ah > aa;
                    boolean actualDraw = ah == aa;
                    boolean actualAway = ah < aa;
                    String actualOutcome = actualHome ? "HOME" : (actualDraw ? "DRAW" : "AWAY");
                    if (actualHome) actualHomeWins++; else if (actualDraw) actualDraws++; else actualAwayWins++;
                    if (predictedOutcome.equals(actualOutcome)) outcomeCorrect++;

                    double hp = record.optDouble("homeP", 0) / 100.0;
                    double dp = record.optDouble("drawP", 0) / 100.0;
                    double ap = record.optDouble("awayP", 0) / 100.0;
                    double topProbability = Math.max(hp, Math.max(dp, ap));
                    int confidenceBin = Math.min(4, Math.max(0, (int) (topProbability * 5.0)));
                    confidenceBinCount[confidenceBin]++;
                    if (predictedOutcome.equals(actualOutcome)) confidenceBinCorrect[confidenceBin]++;
                    outcomeBrierTotal += square(hp - (actualHome ? 1.0 : 0.0))
                        + square(dp - (actualDraw ? 1.0 : 0.0))
                        + square(ap - (actualAway ? 1.0 : 0.0));

                    boolean actualOver = ah + aa > 2;
                    double overProbability = record.optDouble("overP", 0) / 100.0;
                    if ((overProbability >= 0.5) == actualOver) goalsCorrect++;
                    goalsBrierTotal += square(overProbability - (actualOver ? 1.0 : 0.0));

                    boolean actualBtts = ah > 0 && aa > 0;
                    double bttsProbability = record.optDouble("bttsP", 0) / 100.0;
                    if ((bttsProbability >= 0.5) == actualBtts) bttsCorrect++;
                    bttsBrierTotal += square(bttsProbability - (actualBtts ? 1.0 : 0.0));

                    if (ah == record.optInt("predictedHome", -2)
                            && aa == record.optInt("predictedAway", -2)) exactCorrect++;

                    summary.append("Actual score: ").append(ah).append(" - ").append(aa).append("\n");
                }
            } else {
                summary.append("Result: ").append(state.equals("PENDING")
                    ? "Waiting for final result" : state).append("\n");
            }
            summary.append("────────────────────\n");
        }

        if (finished > 0) {
            String sampleGuidance = finished < 30
                ? "Sample note: VERY SMALL sample. Treat these metrics as preliminary; results can swing sharply."
                : (finished < 100
                    ? "Sample note: DEVELOPING sample. Look for the same pattern across more matches."
                    : "Sample note: LARGER sample. Metrics are more informative, but still do not guarantee future accuracy.");
            summary.insert("PREDICTION HISTORY\n".length(),
                "Evaluated matches: " + finished + "\n" +
                sampleGuidance + "\n\n" +
                "Majority-result baseline (retrospective): " + Math.max(actualHomeWins, Math.max(actualDraws, actualAwayWins)) + "/" + finished + " (" + String.format(Locale.US, "%.1f", 100.0 * Math.max(actualHomeWins, Math.max(actualDraws, actualAwayWins)) / finished) + "%)\n" +
                "Actual outcomes: home " + actualHomeWins + ", draw " + actualDraws + ", away " + actualAwayWins + "\n" +
                "Outcome pick correct: " + outcomeCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * outcomeCorrect / finished) + "%)\n" +
                "Over/Under pick correct: " + goalsCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * goalsCorrect / finished) + "%)\n" +
                "BTTS pick correct: " + bttsCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * bttsCorrect / finished) + "%)\n" +
                "Exact score: " + exactCorrect + "/" + finished + "\n" +
                "Mean absolute home-goal error: " + String.format(Locale.US, "%.2f", homeGoalAbsError / finished) + "\n" +
                "Mean absolute away-goal error: " + String.format(Locale.US, "%.2f", awayGoalAbsError / finished) + "\n" +
                "Mean absolute total-goal error: " + String.format(Locale.US, "%.2f", totalGoalAbsError / finished) + "\n\n" +
                "CONFIDENCE CALIBRATION (highest 1X2 probability)\n" +
                "0-19%: " + calibrationText(confidenceBinCount[0], confidenceBinCorrect[0]) + "\n" +
                "20-39%: " + calibrationText(confidenceBinCount[1], confidenceBinCorrect[1]) + "\n" +
                "40-59%: " + calibrationText(confidenceBinCount[2], confidenceBinCorrect[2]) + "\n" +
                "60-79%: " + calibrationText(confidenceBinCount[3], confidenceBinCorrect[3]) + "\n" +
                "80-100%: " + calibrationText(confidenceBinCount[4], confidenceBinCorrect[4]) + "\n" +
                "Small bins are noisy; compare predicted confidence with observed hit rate.\n\n" +
                "Probability data check: " + invalidProbabilityRecords + " records have missing, out-of-range or non-normalized 1X2 probabilities.\n\n" +
                "HOW TO READ THESE METRICS\\n" +
                "Hit rate measures pick frequency; Brier score measures probability quality, and lower is better. " +
                "The majority baseline is retrospective, not a live strategy. Results depend on saved sample size and competition mix.\\n\\n" +
                "PROBABILITY QUALITY (Brier score; lower is better)\n" +
                "Home/Draw/Away: " + String.format(Locale.US, "%.3f", outcomeBrierTotal / finished) + "\n" +
                "Over 2.5: " + String.format(Locale.US, "%.3f", goalsBrierTotal / finished) + "\n" +
                "Both teams score: " + String.format(Locale.US, "%.3f", bttsBrierTotal / finished) + "\n\n");
        } else {
            summary.append("\nNo saved predictions have a final result yet. Save predictions for upcoming matches, then use this button again after they finish.\n");
        }

        new AlertDialog.Builder(this)
            .setTitle("Football Genie • Prediction History")
            .setMessage(summary.toString())
            .setPositiveButton("Close", null)
            .show();
    }

    private void loadFixtures() {
        Toast.makeText(
            MainActivity.this,
            "Loading fixtures...",
            Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                    "https://football-genie-api.onrender.com/api/matches"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject data = new JSONObject(response.toString());
                JSONArray matches = data.getJSONArray("matches");

                ArrayList<JSONObject> fixtures = new ArrayList<>();
                ArrayList<String> fixtureNames = new ArrayList<>();

                for (int i = 0; i < matches.length(); i++) {
                    JSONObject match = matches.getJSONObject(i);
                    JSONObject home = match.optJSONObject("homeTeam");
                    JSONObject away = match.optJSONObject("awayTeam");

                    if (home == null || away == null) {
                        continue;
                    }

                    String homeName = home.optString("name", "Home team");
                    String awayName = away.optString("name", "Away team");
                    String status = match.optString("status", "SCHEDULED");
                    String competition = "";
                    JSONObject comp = match.optJSONObject("competition");

                    if (comp != null) {
                        competition = comp.optString("name", "");
                    }

                    String date = match.optString("utcDate", "");
                    String display = homeName + " vs " + awayName;

                    if (!competition.isEmpty()) {
                        display += "\n" + competition;
                    }

                    if (date.length() >= 16) {
                        display += "\nDate: "
                            + date.substring(0, 10)
                            + " "
                            + date.substring(11, 16)
                            + " UTC";
                    }

                    String statusLabel;
                    switch (status) {
                        case "IN_PLAY":
                            statusLabel = "LIVE";
                            break;
                        case "PAUSED":
                            statusLabel = "LIVE - Half-time or paused";
                            break;
                        case "EXTRA_TIME":
                            statusLabel = "LIVE - Extra time";
                            break;
                        case "PENALTY_SHOOTOUT":
                            statusLabel = "LIVE - Penalty shoot-out";
                            break;
                        case "FINISHED":
                            statusLabel = "Completed";
                            break;
                        case "POSTPONED":
                            statusLabel = "Postponed";
                            break;
                        case "SUSPENDED":
                            statusLabel = "Suspended";
                            break;
                        case "CANCELLED":
                            statusLabel = "Cancelled";
                            break;
                        case "AWARDED":
                            statusLabel = "Result awarded";
                            break;
                        case "TIMED":
                        case "SCHEDULED":
                        default:
                            statusLabel = "Upcoming";
                            break;
                    }

                    display += "\nStatus: " + statusLabel;

                    JSONObject score = match.optJSONObject("score");
                    if (score != null) {
                        JSONObject scoreDetails = score.optJSONObject("fullTime");
                        if (scoreDetails == null
                                || scoreDetails.isNull("home")
                                || scoreDetails.isNull("away")) {
                            scoreDetails = score.optJSONObject("regularTime");
                        }
                        if (scoreDetails == null
                                || scoreDetails.isNull("home")
                                || scoreDetails.isNull("away")) {
                            scoreDetails = score.optJSONObject("halfTime");
                        }

                        if (scoreDetails != null
                                && !scoreDetails.isNull("home")
                                && !scoreDetails.isNull("away")) {
                            display += "\nScore: "
                                + scoreDetails.optInt("home")
                                + " - "
                                + scoreDetails.optInt("away");
                        }
                    }

                    fixtures.add(match);
                    fixtureNames.add(display);
                }

                runOnUiThread(() -> {
                    loadedFixtures.clear();
                    loadedFixtures.addAll(fixtures);
                    if (fixtureListStatus != null) {
                        fixtureListStatus.setText(fixtures.size() + " fixtures loaded");
                    }
                    renderFixtureCards(loadedFixtures);
                    if (fixtures.isEmpty()) {
                        Toast.makeText(
                            MainActivity.this,
                            "No matches found in the available date range.",
                            Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                            MainActivity.this,
                            fixtures.size() + " fixtures loaded in Live Fixtures.",
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "Could not load fixtures. Please try again.",
                    Toast.LENGTH_LONG
                ).show());

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // Estimate expected goals from each team's recent completed matches.
    private void loadTeamStatsForFixture(JSONObject fixture) {
        new Thread(() -> {
            try {
                String homeId = String.valueOf(fixture.getJSONObject("homeTeam").getLong("id"));
                String awayId = String.valueOf(fixture.getJSONObject("awayTeam").getLong("id"));
                JSONObject homeStats = fetchTeamStats(homeId);
                JSONObject awayStats = fetchTeamStats(awayId);
                JSONObject homeGoalsStats = homeStats.optJSONObject("goals");
                JSONObject awayGoalsStats = awayStats.optJSONObject("goals");

                if (homeGoalsStats == null || awayGoalsStats == null
                        || homeStats.optInt("sampleSize", 0) == 0
                        || awayStats.optInt("sampleSize", 0) == 0) {
                    runOnUiThread(() -> {
                        renderRecentFormMessage("STATISTICS UNAVAILABLE",
                            "Recent results could not be loaded for one or both teams. Enter expected goals manually.");
                        Toast.makeText(
                            MainActivity.this,
                            "Recent stats unavailable. You can enter expected goals manually.",
                            Toast.LENGTH_LONG
                        ).show();
                    });
                    return;
                }

                // Recent matches are ordered newest first. Weight current form more heavily.
                JSONArray homeRecentMatches = homeStats.optJSONArray("matches");
                JSONArray awayRecentMatches = awayStats.optJSONArray("matches");
                double homeScored = weightedRecentAverage(homeRecentMatches, true, null);
                double homeConceded = weightedRecentAverage(homeRecentMatches, false, null);
                double awayScored = weightedRecentAverage(awayRecentMatches, true, null);
                double awayConceded = weightedRecentAverage(awayRecentMatches, false, null);
                if (homeScored < 0) homeScored = homeGoalsStats.optDouble("averageScored", 0);
                if (homeConceded < 0) homeConceded = homeGoalsStats.optDouble("averageConceded", 0);
                if (awayScored < 0) awayScored = awayGoalsStats.optDouble("averageScored", 0);
                if (awayConceded < 0) awayConceded = awayGoalsStats.optDouble("averageConceded", 0);

                JSONObject homeVenueStats = homeStats.optJSONObject("venueStats");
                JSONObject awayVenueStats = awayStats.optJSONObject("venueStats");
                JSONObject homeAtHome = homeVenueStats != null
                    ? homeVenueStats.optJSONObject("home") : null;
                JSONObject awayAway = awayVenueStats != null
                    ? awayVenueStats.optJSONObject("away") : null;

                int homeVenueSample = homeAtHome != null
                    ? homeAtHome.optInt("sampleSize", 0) : 0;
                int awayVenueSample = awayAway != null
                    ? awayAway.optInt("sampleSize", 0) : 0;

                // Blend venue-specific rates with overall recent rates.
                // Small venue samples receive less weight to reduce overreaction.
                double homeVenueWeight = homeVenueSample / (homeVenueSample + 4.0);
                double awayVenueWeight = awayVenueSample / (awayVenueSample + 4.0);

                double homeVenueScored = weightedRecentAverage(homeRecentMatches, true, "HOME");
                double homeVenueConceded = weightedRecentAverage(homeRecentMatches, false, "HOME");
                double awayVenueScored = weightedRecentAverage(awayRecentMatches, true, "AWAY");
                double awayVenueConceded = weightedRecentAverage(awayRecentMatches, false, "AWAY");
                if (homeVenueScored < 0) homeVenueScored = homeAtHome != null
                    ? homeAtHome.optDouble("averageScored", homeScored) : homeScored;
                if (homeVenueConceded < 0) homeVenueConceded = homeAtHome != null
                    ? homeAtHome.optDouble("averageConceded", homeConceded) : homeConceded;
                if (awayVenueScored < 0) awayVenueScored = awayAway != null
                    ? awayAway.optDouble("averageScored", awayScored) : awayScored;
                if (awayVenueConceded < 0) awayVenueConceded = awayAway != null
                    ? awayAway.optDouble("averageConceded", awayConceded) : awayConceded;

                double homeAttack = homeScored * (1.0 - homeVenueWeight)
                    + homeVenueScored * homeVenueWeight;
                double homeDefence = homeConceded * (1.0 - homeVenueWeight)
                    + homeVenueConceded * homeVenueWeight;
                double awayAttack = awayScored * (1.0 - awayVenueWeight)
                    + awayVenueScored * awayVenueWeight;
                double awayDefence = awayConceded * (1.0 - awayVenueWeight)
                    + awayVenueConceded * awayVenueWeight;

                // Combine attack and opposing defence multiplicatively. The geometric
                // mean limits overreaction when one rate is unusually high or low.
                // A small floor keeps zero-goal samples from collapsing expected goals.
                double homeExpected = Math.sqrt(
                    Math.max(0.10, homeAttack) * Math.max(0.10, awayDefence)
                ) * 1.05;
                double awayExpected = Math.sqrt(
                    Math.max(0.10, awayAttack) * Math.max(0.10, homeDefence)
                );

                // When available, blend in current competition home/away scoring
                // rates and season attack/defence strength to account for opposition.
                String competitionId = "";
                JSONObject competition = fixture.optJSONObject("competition");
                if (competition != null && competition.optLong("id", 0) > 0) {
                    competitionId = String.valueOf(competition.optLong("id"));
                }
                JSONObject competitionStrength = null;
                if (!competitionId.isEmpty()) {
                    try {
                        competitionStrength = fetchCompetitionStrength(competitionId);
                    } catch (Exception ignored) {
                        // Keep the recent-form estimate if standings are unavailable.
                    }
                }
                JSONObject homeSeason = findCompetitionTeamStats(
                    competitionStrength, homeId
                );
                JSONObject awaySeason = findCompetitionTeamStats(
                    competitionStrength, awayId
                );
                boolean usedCompetitionStrength = homeSeason != null
                    && awaySeason != null
                    && competitionStrength != null
                    && competitionStrength.optDouble("homeLeagueGoalsPerMatch", 0) > 0
                    && competitionStrength.optDouble("awayLeagueGoalsPerMatch", 0) > 0;

                // Shrink small recent-form samples toward competition scoring baselines.
                // As more completed matches become available, the observed team rates carry
                // more weight. Venue-specific rates are separately sample-weighted below.
                if (usedCompetitionStrength) {
                    double leagueHomeBaseline = competitionStrength.optDouble(
                        "homeLeagueGoalsPerMatch", 0
                    );
                    double leagueAwayBaseline = competitionStrength.optDouble(
                        "awayLeagueGoalsPerMatch", 0
                    );
                    int homeRecentSample = homeStats.optInt("sampleSize", 0);
                    int awayRecentSample = awayStats.optInt("sampleSize", 0);
                    double homeRecentReliability = homeRecentSample
                        / (homeRecentSample + 4.0);
                    double awayRecentReliability = awayRecentSample
                        / (awayRecentSample + 4.0);
                    homeScored = homeScored * homeRecentReliability
                        + leagueHomeBaseline * (1.0 - homeRecentReliability);
                    homeConceded = homeConceded * homeRecentReliability
                        + leagueAwayBaseline * (1.0 - homeRecentReliability);
                    awayScored = awayScored * awayRecentReliability
                        + leagueAwayBaseline * (1.0 - awayRecentReliability);
                    awayConceded = awayConceded * awayRecentReliability
                        + leagueHomeBaseline * (1.0 - awayRecentReliability);
                    homeAttack = homeScored * (1.0 - homeVenueWeight)
                        + homeVenueScored * homeVenueWeight;
                    homeDefence = homeConceded * (1.0 - homeVenueWeight)
                        + homeVenueConceded * homeVenueWeight;
                    awayAttack = awayScored * (1.0 - awayVenueWeight)
                        + awayVenueScored * awayVenueWeight;
                    awayDefence = awayConceded * (1.0 - awayVenueWeight)
                        + awayVenueConceded * awayVenueWeight;
                }

                // Partly normalize recent goal rates for the strength of opponents faced.
                // This is a conservative adjustment using current season venue rates;
                // it falls back to unadjusted form when opponent IDs/stats are missing.
                boolean usedOpponentAdjustment = false;
                if (usedCompetitionStrength) {
                    double adjustedHomeScored = opponentAdjustedRecentAverage(
                        homeRecentMatches, true, competitionStrength, competitionId, null
                    );
                    double adjustedHomeConceded = opponentAdjustedRecentAverage(
                        homeRecentMatches, false, competitionStrength, competitionId, null
                    );
                    double adjustedAwayScored = opponentAdjustedRecentAverage(
                        awayRecentMatches, true, competitionStrength, competitionId, null
                    );
                    double adjustedAwayConceded = opponentAdjustedRecentAverage(
                        awayRecentMatches, false, competitionStrength, competitionId, null
                    );
                    usedOpponentAdjustment = adjustedHomeScored >= 0
                        || adjustedHomeConceded >= 0
                        || adjustedAwayScored >= 0
                        || adjustedAwayConceded >= 0;
                    if (adjustedHomeScored >= 0) homeScored = 0.5 * homeScored + 0.5 * adjustedHomeScored;
                    if (adjustedHomeConceded >= 0) homeConceded = 0.5 * homeConceded + 0.5 * adjustedHomeConceded;
                    if (adjustedAwayScored >= 0) awayScored = 0.5 * awayScored + 0.5 * adjustedAwayScored;
                    if (adjustedAwayConceded >= 0) awayConceded = 0.5 * awayConceded + 0.5 * adjustedAwayConceded;

                    homeAttack = homeScored * (1.0 - homeVenueWeight)
                        + homeVenueScored * homeVenueWeight;
                    homeDefence = homeConceded * (1.0 - homeVenueWeight)
                        + homeVenueConceded * homeVenueWeight;
                    awayAttack = awayScored * (1.0 - awayVenueWeight)
                        + awayVenueScored * awayVenueWeight;
                    awayDefence = awayConceded * (1.0 - awayVenueWeight)
                        + awayVenueConceded * awayVenueWeight;
                }

                if (usedCompetitionStrength) {
                    double adjustedHomeVenueScored = opponentAdjustedRecentAverage(
                        homeRecentMatches, true, competitionStrength, competitionId, "HOME"
                    );
                    double adjustedHomeVenueConceded = opponentAdjustedRecentAverage(
                        homeRecentMatches, false, competitionStrength, competitionId, "HOME"
                    );
                    double adjustedAwayVenueScored = opponentAdjustedRecentAverage(
                        awayRecentMatches, true, competitionStrength, competitionId, "AWAY"
                    );
                    double adjustedAwayVenueConceded = opponentAdjustedRecentAverage(
                        awayRecentMatches, false, competitionStrength, competitionId, "AWAY"
                    );
                    if (adjustedHomeVenueScored >= 0) {
                        homeVenueScored = 0.5 * homeVenueScored + 0.5 * adjustedHomeVenueScored;
                    }
                    if (adjustedHomeVenueConceded >= 0) {
                        homeVenueConceded = 0.5 * homeVenueConceded + 0.5 * adjustedHomeVenueConceded;
                    }
                    if (adjustedAwayVenueScored >= 0) {
                        awayVenueScored = 0.5 * awayVenueScored + 0.5 * adjustedAwayVenueScored;
                    }
                    if (adjustedAwayVenueConceded >= 0) {
                        awayVenueConceded = 0.5 * awayVenueConceded + 0.5 * adjustedAwayVenueConceded;
                    }
                    homeAttack = homeScored * (1.0 - homeVenueWeight)
                        + homeVenueScored * homeVenueWeight;
                    homeDefence = homeConceded * (1.0 - homeVenueWeight)
                        + homeVenueConceded * homeVenueWeight;
                    awayAttack = awayScored * (1.0 - awayVenueWeight)
                        + awayVenueScored * awayVenueWeight;
                    awayDefence = awayConceded * (1.0 - awayVenueWeight)
                        + awayVenueConceded * awayVenueWeight;
                }

                if (usedCompetitionStrength) {
                    double leagueHomeGoals = competitionStrength.optDouble(
                        "homeLeagueGoalsPerMatch", 1.45
                    );
                    double leagueAwayGoals = competitionStrength.optDouble(
                        "awayLeagueGoalsPerMatch", 1.15
                    );
                    double homeAttackRatio = homeSeason.optDouble("homeScored", leagueHomeGoals)
                        / leagueHomeGoals;
                    double awayDefenceRatio = awaySeason.optDouble("awayConceded", leagueHomeGoals)
                        / leagueHomeGoals;
                    double awayAttackRatio = awaySeason.optDouble("awayScored", leagueAwayGoals)
                        / leagueAwayGoals;
                    double homeDefenceRatio = homeSeason.optDouble("homeConceded", leagueAwayGoals)
                        / leagueAwayGoals;

                    homeAttackRatio = Math.max(0.55, Math.min(1.70, homeAttackRatio));
                    awayDefenceRatio = Math.max(0.55, Math.min(1.70, awayDefenceRatio));
                    awayAttackRatio = Math.max(0.55, Math.min(1.70, awayAttackRatio));
                    homeDefenceRatio = Math.max(0.55, Math.min(1.70, homeDefenceRatio));

                    double seasonHomeExpected = leagueHomeGoals
                        * Math.sqrt(homeAttackRatio * awayDefenceRatio);
                    double seasonAwayExpected = leagueAwayGoals
                        * Math.sqrt(awayAttackRatio * homeDefenceRatio);

                    // Trust season strength more as the relevant home/away sample grows.
                    // Early-season standings are noisy, so shrink them toward recent form.
                    int homeSeasonGames = homeSeason.optInt("homePlayed", 0);
                    int awaySeasonGames = awaySeason.optInt("awayPlayed", 0);
                    int reliableSeasonGames = Math.max(0,
                        Math.min(homeSeasonGames, awaySeasonGames));
                    double seasonWeight = 0.35 * reliableSeasonGames
                        / (reliableSeasonGames + 4.0);
                    double recentFormWeight = 1.0 - seasonWeight;
                    homeExpected = homeExpected * recentFormWeight
                        + seasonHomeExpected * seasonWeight;
                    awayExpected = awayExpected * recentFormWeight
                        + seasonAwayExpected * seasonWeight;
                }

                homeExpected = Math.max(0.2, Math.min(4.5, homeExpected));
                awayExpected = Math.max(0.2, Math.min(4.5, awayExpected));

                final String homeValue = String.format(Locale.US, "%.2f", homeExpected);
                final String awayValue = String.format(Locale.US, "%.2f", awayExpected);
                final String homeForm = homeStats.optString("form", "Not available");
                final String awayForm = awayStats.optString("form", "Not available");
                final JSONObject homeTeamStats = homeStats.optJSONObject("team");
                final JSONObject awayTeamStats = awayStats.optJSONObject("team");
                final String homeName = homeTeamStats != null
                    ? homeTeamStats.optString("name", "Home team") : "Home team";
                final String awayName = awayTeamStats != null
                    ? awayTeamStats.optString("name", "Away team") : "Away team";
                final int homeSample = homeStats.optInt("sampleSize", 0);
                final int awaySample = awayStats.optInt("sampleSize", 0);
                final int displayedHomeVenueSample = homeVenueSample;
                final int displayedAwayVenueSample = awayVenueSample;
                final boolean appliedOpponentAdjustment = usedOpponentAdjustment;
                final int homeOpponentSample = countRatedOpponents(
                    homeRecentMatches, competitionStrength, competitionId
                );
                final int awayOpponentSample = countRatedOpponents(
                    awayRecentMatches, competitionStrength, competitionId
                );
                final String dataQualitySummary;
                int smallerRecentSample = Math.min(homeSample, awaySample);
                if (smallerRecentSample >= 8) {
                    dataQualitySummary = "Recent-form sample: reasonably developed for both teams.";
                } else if (smallerRecentSample >= 5) {
                    dataQualitySummary = "Recent-form sample: moderate; use extra caution.";
                } else {
                    dataQualitySummary = "Recent-form sample: limited; expected goals may be unstable.";
                }
                final String opponentCoverageSummary =
                    "Opponent ratings available for " + homeOpponentSample + "/" +
                    homeSample + " recent " + homeName + " matches and " +
                    awayOpponentSample + "/" + awaySample + " recent " + awayName +
                    " matches. Missing ratings fall back to unadjusted form.";
                final String homeWeightedScoredText = String.format(Locale.US, "%.2f", homeScored);
                final String homeWeightedConcededText = String.format(Locale.US, "%.2f", homeConceded);
                final String awayWeightedScoredText = String.format(Locale.US, "%.2f", awayScored);
                final String awayWeightedConcededText = String.format(Locale.US, "%.2f", awayConceded);
                final String homeScoringTrend = recentTrendText(homeRecentMatches, true);
                final String homeConcedingTrend = recentTrendText(homeRecentMatches, false);
                final String awayScoringTrend = recentTrendText(awayRecentMatches, true);
                final String awayConcedingTrend = recentTrendText(awayRecentMatches, false);
                final String relativeStrengthSummary;
                if (usedCompetitionStrength) {
                    double leagueHomeBaseline = competitionStrength.optDouble(
                        "homeLeagueGoalsPerMatch", 1.45
                    );
                    double leagueAwayBaseline = competitionStrength.optDouble(
                        "awayLeagueGoalsPerMatch", 1.15
                    );
                    relativeStrengthSummary =
                        "Relative attack / defensive-vulnerability indices (1.00 = competition baseline):\n" +
                        homeName + " attack " + String.format(Locale.US, "%.2f", homeAttack / leagueHomeBaseline) +
                        "x; vulnerability " + String.format(Locale.US, "%.2f", homeDefence / leagueAwayBaseline) + "x\n" +
                        awayName + " attack " + String.format(Locale.US, "%.2f", awayAttack / leagueAwayBaseline) +
                        "x; vulnerability " + String.format(Locale.US, "%.2f", awayDefence / leagueHomeBaseline) + "x.\n" +
                        "Higher vulnerability means conceding more goals than the relevant baseline.";
                } else {
                    relativeStrengthSummary =
                        "Relative attack/defence indices unavailable because competition baselines could not be loaded.";
                }

                runOnUiThread(() -> {
                    homeGoals.setText(homeValue);
                    awayGoals.setText(awayValue);
                    renderRecentFormData(
                        homeName, homeForm, homeSample,
                        homeGoalsStats.optString("averageScored", "0"),
                        homeGoalsStats.optString("averageConceded", "0"),
                        homeWeightedScoredText, homeWeightedConcededText,
                        homeValue, homeScoringTrend, homeConcedingTrend,
                        awayName, awayForm, awaySample,
                        awayGoalsStats.optString("averageScored", "0"),
                        awayGoalsStats.optString("averageConceded", "0"),
                        awayWeightedScoredText, awayWeightedConcededText,
                        awayValue, awayScoringTrend, awayConcedingTrend,
                        displayedHomeVenueSample, displayedAwayVenueSample,
                        relativeStrengthSummary,
                        (usedCompetitionStrength
                            ? "Competition baselines and season home/away strength are included. "
                            : "Competition standings unavailable; season-strength adjustment skipped. ") +
                        (appliedOpponentAdjustment
                            ? "Recent form includes available opponent-strength adjustments. "
                            : "Opponent-strength ratings were unavailable for recent-form adjustment. ") +
                        dataQualitySummary + " " + opponentCoverageSummary +
                        " Estimates only—check team news and adjust xG if needed."
                    );
                    Toast.makeText(
                        MainActivity.this,
                        "Recent team statistics loaded.",
                        Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    renderRecentFormMessage("COULD NOT LOAD TEAM FORM",
                        "Check your connection and try again, or enter expected goals manually.");
                    Toast.makeText(
                        MainActivity.this,
                        "Could not load team stats. Manual entry is still available.",
                        Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }


    private void renderRecentFormMessage(String title, String message) {
        if (statsSummary == null) return;
        statsSummary.removeAllViews();
        TextView heading = label(title);
        heading.setTextColor(GREEN);
        heading.setTextSize(11);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, 0, 0, dp(6));
        statsSummary.addView(heading);
        TextView body = label(message);
        body.setTextColor(MUTED);
        body.setTextSize(12);
        body.setLineSpacing(dp(3), 1.0f);
        statsSummary.addView(body);
    }

    private void renderRecentFormData(
            String homeName, String homeForm, int homeSample,
            String homeScored, String homeConceded, String homeWeightedScored,
            String homeWeightedConceded, String homeXg, String homeScoringTrend,
            String homeConcedingTrend,
            String awayName, String awayForm, int awaySample,
            String awayScored, String awayConceded, String awayWeightedScored,
            String awayWeightedConceded, String awayXg, String awayScoringTrend,
            String awayConcedingTrend, int homeVenueSample, int awayVenueSample,
            String relativeStrength, String modelNotes) {
        if (statsSummary == null) return;
        statsSummary.removeAllViews();

        TextView caption = label("LAST MATCHES  •  MOST RECENT FIRST");
        caption.setTextColor(MUTED);
        caption.setTextSize(10);
        caption.setTypeface(null, Typeface.BOLD);
        caption.setPadding(0, 0, 0, dp(10));
        statsSummary.addView(caption);

        addRecentTeamFormCard(statsSummary, homeName, homeForm, homeSample,
            homeScored, homeConceded, homeWeightedScored, homeWeightedConceded,
            homeXg, homeScoringTrend, homeConcedingTrend, true);
        addRecentTeamFormCard(statsSummary, awayName, awayForm, awaySample,
            awayScored, awayConceded, awayWeightedScored, awayWeightedConceded,
            awayXg, awayScoringTrend, awayConcedingTrend, false);

        TextView venue = label("VENUE SPLIT   Home team at home: " + homeVenueSample
            + " matches   •   Away team away: " + awayVenueSample + " matches");
        venue.setTextColor(MUTED);
        venue.setTextSize(10);
        venue.setPadding(dp(2), dp(8), dp(2), dp(2));
        venue.setLineSpacing(dp(2), 1.0f);
        statsSummary.addView(venue);

        TextView details = label("＋  MODEL NOTES & DATA QUALITY");
        details.setTextColor(GREEN);
        details.setTextSize(11);
        details.setTypeface(null, Typeface.BOLD);
        details.setPadding(dp(2), dp(12), dp(2), dp(7));
        details.setClickable(true);
        statsSummary.addView(details);

        LinearLayout detailPanel = new LinearLayout(this);
        detailPanel.setOrientation(LinearLayout.VERTICAL);
        detailPanel.setPadding(dp(10), dp(9), dp(10), dp(9));
        detailPanel.setBackground(rounded(Color.rgb(17, 34, 51), 10));
        TextView strength = label(relativeStrength);
        strength.setTextColor(WHITE);
        strength.setTextSize(11);
        strength.setLineSpacing(dp(3), 1.0f);
        detailPanel.addView(strength);
        TextView notes = label(modelNotes);
        notes.setTextColor(MUTED);
        notes.setTextSize(10);
        notes.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(8);
        detailPanel.addView(notes, noteParams);
        detailPanel.setVisibility(View.GONE);
        statsSummary.addView(detailPanel);
        details.setOnClickListener(v -> {
            boolean show = detailPanel.getVisibility() != View.VISIBLE;
            detailPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            details.setText(show ? "−  HIDE MODEL NOTES & DATA QUALITY"
                : "＋  MODEL NOTES & DATA QUALITY");
        });
    }

    private void addRecentTeamFormCard(LinearLayout parent, String teamName, String form,
            int sample, String overallScored, String overallConceded,
            String weightedScored, String weightedConceded, String xg,
            String scoringTrend, String concedingTrend, boolean homeTeam) {
        LinearLayout teamCard = new LinearLayout(this);
        teamCard.setOrientation(LinearLayout.VERTICAL);
        teamCard.setPadding(dp(10), dp(10), dp(10), dp(10));
        teamCard.setBackground(rounded(Color.rgb(16, 37, 57), 12));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(9);
        parent.addView(teamCard, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        teamCard.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView team = label(teamName);
        team.setTextColor(WHITE);
        team.setTextSize(13);
        team.setTypeface(null, Typeface.BOLD);
        team.setMaxLines(2);
        team.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(team, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView venueTag = label(homeTeam ? "HOME" : "AWAY");
        venueTag.setTextColor(homeTeam ? GREEN : Color.rgb(105, 190, 255));
        venueTag.setTextSize(9);
        venueTag.setTypeface(null, Typeface.BOLD);
        venueTag.setGravity(Gravity.CENTER);
        venueTag.setPadding(dp(8), dp(4), dp(8), dp(4));
        venueTag.setBackground(rounded(Color.rgb(28, 54, 70), 8));
        header.addView(venueTag);

        TextView sampleLabel = label(sample + " matches analysed");
        sampleLabel.setTextColor(MUTED);
        sampleLabel.setTextSize(10);
        sampleLabel.setPadding(0, dp(3), 0, dp(7));
        teamCard.addView(sampleLabel);

        LinearLayout formRow = new LinearLayout(this);
        formRow.setOrientation(LinearLayout.HORIZONTAL);
        formRow.setGravity(Gravity.CENTER_VERTICAL);
        teamCard.addView(formRow, new LinearLayout.LayoutParams(-1, -2));
        String upperForm = form == null ? "" : form.toUpperCase(Locale.US);
        String cleanedForm = upperForm.replaceAll("[^WDL]", "");
        if (cleanedForm.isEmpty() || upperForm.contains("NOT") || upperForm.contains("UNAVAILABLE")) {
            TextView noForm = label("Recent form unavailable");
            noForm.setTextColor(MUTED);
            noForm.setTextSize(11);
            formRow.addView(noForm);
        } else {
            for (int i = 0; i < cleanedForm.length(); i++) {
                String outcome = cleanedForm.substring(i, i + 1);
                int tone = "W".equals(outcome) ? GREEN
                    : ("D".equals(outcome) ? Color.rgb(135, 163, 187)
                    : Color.rgb(255, 91, 105));
                TextView badge = label(outcome);
                badge.setTextColor(Color.rgb(7, 23, 20));
                badge.setTextSize(10);
                badge.setTypeface(null, Typeface.BOLD);
                badge.setGravity(Gravity.CENTER);
                badge.setBackground(rounded(tone, 6));
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(25), dp(25));
                badgeParams.rightMargin = dp(5);
                formRow.addView(badge, badgeParams);
            }
        }

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams metricsParams = new LinearLayout.LayoutParams(-1, -2);
        metricsParams.topMargin = dp(10);
        teamCard.addView(metrics, metricsParams);
        addRecentMetric(metrics, "SCORED / GAME", weightedScored, GREEN);
        addRecentMetric(metrics, "CONCEDED / GAME", weightedConceded, Color.rgb(255, 117, 125));

        LinearLayout xgRow = new LinearLayout(this);
        xgRow.setOrientation(LinearLayout.HORIZONTAL);
        xgRow.setGravity(Gravity.CENTER_VERTICAL);
        xgRow.setPadding(dp(9), dp(8), dp(9), dp(8));
        xgRow.setBackground(rounded(Color.rgb(7, 24, 38), 8));
        LinearLayout.LayoutParams xgParams = new LinearLayout.LayoutParams(-1, -2);
        xgParams.topMargin = dp(9);
        teamCard.addView(xgRow, xgParams);
        TextView xgLabel = label("ESTIMATED xG");
        xgLabel.setTextColor(MUTED);
        xgLabel.setTextSize(10);
        xgLabel.setTypeface(null, Typeface.BOLD);
        xgRow.addView(xgLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView xgValue = label(xg);
        xgValue.setTextColor(GREEN);
        xgValue.setTextSize(18);
        xgValue.setTypeface(null, Typeface.BOLD);
        xgRow.addView(xgValue);

        TextView trend = label("Scoring trend: " + scoringTrend + "   •   Conceding: " + concedingTrend);
        trend.setTextColor(MUTED);
        trend.setTextSize(10);
        trend.setLineSpacing(dp(2), 1.0f);
        trend.setPadding(dp(1), dp(8), dp(1), 0);
        teamCard.addView(trend);

        TextView baseline = label("Overall average: " + overallScored + " scored  •  " + overallConceded + " conceded per match");
        baseline.setTextColor(Color.rgb(155, 177, 194));
        baseline.setTextSize(9);
        baseline.setPadding(dp(1), dp(5), dp(1), 0);
        teamCard.addView(baseline);
    }

    private void addRecentMetric(LinearLayout row, String title, String value, int accent) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setPadding(dp(8), dp(8), dp(8), dp(8));
        metric.setBackground(rounded(Color.rgb(9, 27, 43), 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.rightMargin = dp(5);
        row.addView(metric, params);
        TextView metricTitle = label(title);
        metricTitle.setTextColor(MUTED);
        metricTitle.setTextSize(8);
        metricTitle.setTypeface(null, Typeface.BOLD);
        metricTitle.setMaxLines(2);
        metric.addView(metricTitle);
        TextView metricValue = label(value);
        metricValue.setTextColor(accent);
        metricValue.setTextSize(17);
        metricValue.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
        valueParams.topMargin = dp(3);
        metric.addView(metricValue, valueParams);
    }

    private JSONObject fetchCompetitionStrength(String competitionId) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(
                "https://football-genie-api.onrender.com/api/competition-strength?competitionId="
                    + competitionId
            );
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new java.io.IOException("Competition strength request failed: " + code);
            }
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();
            return new JSONObject(body.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private JSONObject findCompetitionTeamStats(JSONObject strength, String teamId) {
        if (strength == null) return null;
        JSONArray teams = strength.optJSONArray("teams");
        if (teams == null) return null;
        long targetId;
        try {
            targetId = Long.parseLong(teamId);
        } catch (NumberFormatException e) {
            return null;
        }
        for (int i = 0; i < teams.length(); i++) {
            JSONObject team = teams.optJSONObject(i);
            if (team != null && team.optLong("teamId", -1) == targetId) {
                return team;
            }
        }
        return null;
    }

    // Compare the five newest completed matches with the preceding five.
    // This is a descriptive signal, not proof that a trend will continue.
    private String recentTrendText(JSONArray matches, boolean forGoals) {
        if (matches == null || matches.length() < 6) return "limited sample";
        double recentTotal = 0.0;
        double previousTotal = 0.0;
        int recentCount = 0;
        int previousCount = 0;
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.optJSONObject(i);
            if (match == null) continue;
            int goals = match.optInt(forGoals ? "goalsFor" : "goalsAgainst", -1);
            if (goals < 0) continue;
            if (recentCount < 5) {
                recentTotal += goals;
                recentCount++;
            } else if (previousCount < 5) {
                previousTotal += goals;
                previousCount++;
            }
            if (recentCount >= 5 && previousCount >= 5) break;
        }
        if (recentCount < 3 || previousCount < 3) return "limited sample";
        double difference = recentTotal / recentCount - previousTotal / previousCount;
        if (difference >= 0.35) return "rising";
        if (difference <= -0.35) return "falling";
        return "stable";
    }

    private int countRatedOpponents(
            JSONArray matches, JSONObject competitionStrength, String targetCompetitionId) {
        if (matches == null || competitionStrength == null) return 0;
        int count = 0;
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.optJSONObject(i);
            if (match == null) continue;
            String matchCompetitionId = match.optString("competitionId", "");
            if (!targetCompetitionId.isEmpty() && !matchCompetitionId.isEmpty()
                    && !targetCompetitionId.equals(matchCompetitionId)) continue;
            long opponentId = match.optLong("opponentId", -1);
            if (opponentId <= 0) continue;
            if (findCompetitionTeamStats(
                    competitionStrength, String.valueOf(opponentId)) != null) count++;
        }
        return count;
    }

    // Normalize recent goals against the current venue-specific strength of opponents.
    // A 50% blend is applied by the caller to limit overreaction to noisy standings.
    private double opponentAdjustedRecentAverage(
            JSONArray matches, boolean forGoals, JSONObject competitionStrength,
            String targetCompetitionId, String venueFilter) {
        if (matches == null || competitionStrength == null) return -1;
        double leagueHome = competitionStrength.optDouble("homeLeagueGoalsPerMatch", 0);
        double leagueAway = competitionStrength.optDouble("awayLeagueGoalsPerMatch", 0);
        if (leagueHome <= 0 || leagueAway <= 0) return -1;

        double weightedTotal = 0.0;
        double totalWeight = 0.0;
        int usable = 0;
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.optJSONObject(i);
            if (match == null) continue;
            String matchVenue = match.optString("venue", "");
            if (venueFilter != null && !venueFilter.equals(matchVenue)) continue;
            String matchCompetitionId = match.optString("competitionId", "");
            // Only compare opponent strengths from the same competition as the fixture.
            // Cup and cross-league matches are not calibrated against league standings.
            if (!targetCompetitionId.isEmpty() && !matchCompetitionId.isEmpty()
                    && !targetCompetitionId.equals(matchCompetitionId)) continue;
            long opponentId = match.optLong("opponentId", -1);
            if (opponentId <= 0) continue;
            JSONObject opponent = findCompetitionTeamStats(
                competitionStrength, String.valueOf(opponentId)
            );
            if (opponent == null) continue;

            boolean ourTeamHome = "HOME".equals(match.optString("venue", ""));
            double opponentAttack = ourTeamHome
                ? opponent.optDouble("awayScored", -1)
                : opponent.optDouble("homeScored", -1);
            double opponentConceded = ourTeamHome
                ? opponent.optDouble("awayConceded", -1)
                : opponent.optDouble("homeConceded", -1);
            double scoringBaseline = ourTeamHome ? leagueHome : leagueAway;
            double concedingBaseline = ourTeamHome ? leagueAway : leagueHome;
            int goals = match.optInt(forGoals ? "goalsFor" : "goalsAgainst", -1);
            if (goals < 0) continue;

            double adjustedGoals;
            int opponentVenueGames = ourTeamHome
                ? opponent.optInt("awayPlayed", 0)
                : opponent.optInt("homePlayed", 0);
            double reliability = opponentVenueGames / (opponentVenueGames + 4.0);
            if (forGoals) {
                if (opponentConceded < 0) continue;
                double rawRatio = Math.max(0.55, Math.min(1.70,
                    opponentConceded / scoringBaseline));
                double ratio = 1.0 + (rawRatio - 1.0) * reliability;
                adjustedGoals = Math.max(0.0, Math.min(6.0, goals / ratio));
            } else {
                if (opponentAttack < 0) continue;
                double rawRatio = Math.max(0.55, Math.min(1.70,
                    opponentAttack / concedingBaseline));
                double ratio = 1.0 + (rawRatio - 1.0) * reliability;
                adjustedGoals = Math.max(0.0, Math.min(6.0, goals / ratio));
            }
            double weight = Math.pow(0.88, usable);
            weightedTotal += adjustedGoals * weight;
            totalWeight += weight;
            usable++;
        }
        return totalWeight > 0 ? weightedTotal / totalWeight : -1;
    }

    // Newest matches receive the greatest weight (1.0, then 0.88, 0.77...).
    // Filter by HOME/AWAY for venue-specific form; null uses all recent matches.
    // Returns -1 if no usable match-level observations are available.
    private double weightedRecentAverage(JSONArray matches, boolean forGoals, String venueFilter) {
        if (matches == null || matches.length() == 0) return -1;
        double weightedTotal = 0.0;
        double totalWeight = 0.0;
        int usableMatches = 0;
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.optJSONObject(i);
            if (match == null) continue;
            if (venueFilter != null && !venueFilter.equals(match.optString("venue", ""))) continue;
            int goals = match.optInt(forGoals ? "goalsFor" : "goalsAgainst", -1);
            if (goals < 0) continue;

            // Weight only usable observations. For venue-filtered rates, the newest
            // matching home/away match must receive full weight even if other venues
            // occurred between matching games.
            double weight = Math.pow(0.88, usableMatches);
            weightedTotal += goals * weight;
            totalWeight += weight;
            usableMatches++;
        }
        return totalWeight > 0 ? weightedTotal / totalWeight : -1;
    }

    private JSONObject fetchTeamStats(String teamId) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(
                "https://football-genie-api.onrender.com/api/team-stats?teamId="
                    + teamId + "&limit=10"
            );
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int code = connection.getResponseCode();
            java.io.InputStream stream = (code >= 200 && code < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();

            if (code < 200 || code >= 300) {
                throw new java.io.IOException("Team stats request failed: " + code);
            }
            return new JSONObject(body.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    }