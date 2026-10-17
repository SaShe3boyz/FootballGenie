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
    private TextView result, status, statsSummary;
    private LinearLayout mainLayout;
    private JSONObject selectedFixture;

    private final int BG = Color.rgb(9, 16, 31);
    private final int CARD = Color.rgb(20, 31, 51);
    private final int GREEN = Color.rgb(80, 230, 150);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(170, 185, 205);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(18), dp(20), dp(18), dp(30));

        scroll.addView(mainLayout);

        buildHeader();
        buildMatchForm();
        buildResults();

        setContentView(scroll);
        checkApiConnection();
    }

    private void buildHeader() {
        TextView badge = new TextView(this);
        badge.setText("⚽  YOUR PERSONAL FOOTBALL ANALYST");
        badge.setTextColor(GREEN);
        badge.setTextSize(11);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(0, dp(8), 0, dp(10));
        mainLayout.addView(badge);

        TextView title = new TextView(this);
        title.setText("FOOTBALL GENIE");
        title.setTextColor(WHITE);
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        mainLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("PREDICT  •  SIMULATE  •  UNDERSTAND");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(11);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(20));
        mainLayout.addView(subtitle);

        LinearLayout banner = card();
        banner.setOrientation(LinearLayout.VERTICAL);

        TextView heading = label("MATCH INTELLIGENCE");
        heading.setTextColor(GREEN);
        heading.setTextSize(12);
        heading.setTypeface(null, Typeface.BOLD);

        banner.addView(heading);

        TextView description = label(
            "Explore match probabilities using expected goals " +
            "and 10,000 simulated match outcomes."
        );
        description.setTextColor(WHITE);
        description.setTextSize(14);
        description.setPadding(0, dp(8), 0, 0);
        banner.addView(description);

        mainLayout.addView(banner);
    }

    private void buildMatchForm() {
        addHeading("01", "MATCH SETUP");

        LinearLayout form = card();
        form.setOrientation(LinearLayout.VERTICAL);

        homeInput = addInput(
            form, "Home team name", false
        );

        awayInput = addInput(
            form, "Away team name", false
        );

        homeGoals = addInput(
            form, "Home expected goals (e.g. 1.6)", true
        );

        awayGoals = addInput(
            form, "Away expected goals (e.g. 1.1)", true
        );

        TextView note = label(
            "Select a fixture to estimate expected goals from recent completed matches. " +
            "You can also edit the values manually."
        );
        note.setTextColor(MUTED);
        note.setTextSize(12);
        note.setPadding(0, dp(8), 0, dp(10));
        form.addView(note);

        statsSummary = label(
            "RECENT TEAM FORM\nSelect a fixture to load recent results and statistics."
        );
        statsSummary.setTextColor(WHITE);
        statsSummary.setTextSize(13);
        statsSummary.setPadding(dp(12), dp(12), dp(12), dp(12));
        statsSummary.setBackground(rounded(Color.rgb(31, 44, 65), 10));
        LinearLayout.LayoutParams statsParams =
            new LinearLayout.LayoutParams(-1, -2);
        statsParams.bottomMargin = dp(12);
        form.addView(statsSummary, statsParams);

                Button fixturesButton = new Button(this);
        fixturesButton.setText("🌐  LOAD LIVE FIXTURES");
        fixturesButton.setTextColor(WHITE);
        fixturesButton.setTextSize(14);
        fixturesButton.setTypeface(null, Typeface.BOLD);
        fixturesButton.setAllCaps(false);
        fixturesButton.setBackground(rounded(CARD, 14));

        LinearLayout.LayoutParams fixturesParams =
            new LinearLayout.LayoutParams(-1, dp(54));

        fixturesParams.bottomMargin = dp(12);

        form.addView(fixturesButton, fixturesParams);

        fixturesButton.setOnClickListener(v -> loadFixtures());

        Button historyButton = new Button(this);
        historyButton.setText("📊  PREDICTION HISTORY / UPDATE RESULTS");
        historyButton.setTextColor(WHITE);
        historyButton.setTextSize(13);
        historyButton.setAllCaps(false);
        historyButton.setBackground(rounded(CARD, 14));
        LinearLayout.LayoutParams historyParams =
            new LinearLayout.LayoutParams(-1, dp(50));
        historyParams.bottomMargin = dp(12);
        form.addView(historyButton, historyParams);
        historyButton.setOnClickListener(v -> syncPredictionHistory());

        Button simulateButton = new Button(this);
        simulateButton.setText("⚽  SIMULATE 10,000 MATCHES");
        simulateButton.setTextColor(Color.rgb(8, 25, 22));
        simulateButton.setTextSize(14);
        simulateButton.setTypeface(null, Typeface.BOLD);
        simulateButton.setAllCaps(false);
        simulateButton.setBackground(rounded(GREEN, 14));

        form.addView(
            simulateButton,
            new LinearLayout.LayoutParams(
                -1, dp(54)
            )
        );

        simulateButton.setOnClickListener(v -> {
            hideKeyboard();
            simulate();
        });

        mainLayout.addView(form);
    }

    private void buildResults() {
        addHeading("02", "PREDICTION CENTRE");

        LinearLayout outputCard = card();
        outputCard.setOrientation(LinearLayout.VERTICAL);

        status = label("AWAITING MATCH SIMULATION");
        status.setTextColor(GREEN);
        status.setTextSize(12);
        status.setTypeface(null, Typeface.BOLD);
        outputCard.addView(status);

        result = label(
            "Enter the two teams and their expected goals, " +
            "then tap the simulation button to see your analysis."
        );
        result.setTextColor(WHITE);
        result.setTextSize(14);
        result.setPadding(0, dp(16), 0, dp(8));

        outputCard.addView(result);
        mainLayout.addView(outputCard);

        TextView footer = label(
            "FOOTBALL GENIE  •  STATISTICS, NOT CERTAINTY\n" +
            "Probabilities describe modelled outcomes. " +
            "Real matches can differ."
        );
        footer.setTextColor(MUTED);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, dp(10));
        mainLayout.addView(footer);
    }

    private void addHeading(String number, String heading) {
        TextView text = label(number + "   " + heading);
        text.setTextColor(GREEN);
        text.setTextSize(14);
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(dp(2), dp(24), 0, dp(12));
        mainLayout.addView(text);
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(rounded(CARD, 18));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);

        params.bottomMargin = dp(8);
        box.setLayoutParams(params);

        return box;
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

            int[][] scores = new int[9][9];

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

                if (hg < 9 && ag < 9) {
                    scores[hg][ag]++;
                }
            }

            int bestHome = 0;
            int bestAway = 0;
            int bestCount = 0;

            for (int h = 0; h < 9; h++) {
                for (int a = 0; a < 9; a++) {
                    if (scores[h][a] > bestCount) {
                        bestCount = scores[h][a];
                        bestHome = h;
                        bestAway = a;
                    }
                }
            }

            double homeP = homeWins / 100.0;
            double drawP = draws / 100.0;
            double awayP = awayWins / 100.0;
            double overP = over25 / 100.0;
            double underP = 100.0 - overP;
            double bttsP = btts / 100.0;

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

            String goalTrend = overP >= 50
                ? "Over 2.5 goals is more likely than Under 2.5."
                : "Under 2.5 goals is more likely than Over 2.5.";

            String bttsTrend = bttsP >= 50
                ? "Both teams scoring is more likely than not."
                : "At least one team failing to score is more likely.";

            String analysis =
                "\n\n━━━━━━━━━━━━━━━━━━\n" +
                "WHY THIS PREDICTION?\n\n" +
                "1. Expected goals\n" +
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
                "2. Most likely match outcome\n" +
                outcome + " is the most likely of the three " +
                "outcomes in this simulation (" +
                String.format(Locale.US, "%.1f", highest) +
                "%). This is not a guarantee.\n\n" +

                "3. Goals analysis\n" + goalTrend +
                "\n" + bttsTrend +
                "\n\n" +

                "4. Most frequent score\n" +
                home + " " + bestHome + " - " +
                bestAway + " " + away +
                " was the most frequent exact score in " +
                "these simulation runs.\n\n" +

                "IMPORTANT LIMITATIONS\n" +
                "Expected goals can be estimated from recent completed matches " +
                "when you select a fixture, or entered manually. This model does " +
                "not currently account for injuries, confirmed line-ups, weather " +
                "or every relevant team statistic. Those factors may change the " +
                "real match outcome.";

            status.setText("✓  SIMULATION COMPLETE");

            result.setText(String.format(Locale.US,
                "10,000 MATCH SIMULATIONS\n\n" +
                "MATCH OUTCOME\n" +
                "%s win       %.1f%%\n" +
                "Draw              %.1f%%\n" +
                "%s win       %.1f%%\n\n" +
                "GOALS MARKETS\n" +
                "Over 2.5 goals       %.1f%%\n" +
                "Under 2.5 goals     %.1f%%\n" +
                "Both teams score    %.1f%%\n\n" +
                "MOST FREQUENT SCORE\n" +
                "%s %d - %d %s\n" +
                "%s\n\n%s",
                home, homeP,
                drawP,
                away, awayP,
                overP, underP, bttsP,
                home, bestHome, bestAway, away,
                "━━━━━━━━━━━━━━━━━━",
                analysis
            ));

            savePredictionIfEligible(home, away, homeXg, awayXg,
                homeP, drawP, awayP, overP, bttsP, bestHome, bestAway);

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

    private void showPredictionHistory(int newlyUpdated) {
        JSONArray history = readPredictionHistory();
        int finished = 0, outcomeCorrect = 0, goalsCorrect = 0;
        int bttsCorrect = 0, exactCorrect = 0;
        StringBuilder summary = new StringBuilder();
        summary.append("PREDICTION HISTORY\\n");
        summary.append("New final results added: ").append(newlyUpdated).append("\\n\\n");

        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject record = history.optJSONObject(i);
            if (record == null) continue;
            String home = record.optString("home", "Home");
            String away = record.optString("away", "Away");
            String state = record.optString("predictionStatus", "PENDING");
            summary.append(home).append(" vs ").append(away).append("\\n");
            summary.append("Expected goals: ")
                .append(String.format(Locale.US, "%.2f", record.optDouble("homeXg", 0)))
                .append(" - ")
                .append(String.format(Locale.US, "%.2f", record.optDouble("awayXg", 0)))
                .append("\\n");

            if ("FINISHED".equals(state)) {
                int ah = record.optInt("actualHome", -1);
                int aa = record.optInt("actualAway", -1);
                if (ah >= 0 && aa >= 0) {
                    finished++;
                    String predictedOutcome = record.optDouble("homeP", 0) >=
                        Math.max(record.optDouble("drawP", 0), record.optDouble("awayP", 0))
                        ? "HOME" : (record.optDouble("drawP", 0) >= record.optDouble("awayP", 0)
                            ? "DRAW" : "AWAY");
                    String actualOutcome = ah > aa ? "HOME" : (ah == aa ? "DRAW" : "AWAY");
                    if (predictedOutcome.equals(actualOutcome)) outcomeCorrect++;

                    boolean predictedOver = record.optDouble("overP", 0) >= 50.0;
                    if (predictedOver == (ah + aa > 2)) goalsCorrect++;
                    boolean predictedBtts = record.optDouble("bttsP", 0) >= 50.0;
                    if (predictedBtts == (ah > 0 && aa > 0)) bttsCorrect++;
                    if (ah == record.optInt("predictedHome", -2)
                            && aa == record.optInt("predictedAway", -2)) exactCorrect++;

                    summary.append("Actual score: ").append(ah).append(" - ").append(aa).append("\\n");
                }
            } else {
                summary.append("Result: ").append(state.equals("PENDING")
                    ? "Waiting for final result" : state).append("\\n");
            }
            summary.append("────────────────────\\n");
        }

        if (finished > 0) {
            summary.insert("PREDICTION HISTORY\\n".length(),
                "Evaluated matches: " + finished + "\\n" +
                "Outcome pick correct: " + outcomeCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * outcomeCorrect / finished) + "%)\\n" +
                "Over/Under pick correct: " + goalsCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * goalsCorrect / finished) + "%)\\n" +
                "BTTS pick correct: " + bttsCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * bttsCorrect / finished) + "%)\\n" +
                "Exact score: " + exactCorrect + "/" + finished + "\\n\\n");
        } else {
            summary.append("\\nNo saved predictions have a final result yet. Save predictions for upcoming matches, then use this button again after they finish.\\n");
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
                    if (fixtures.isEmpty()) {
                        Toast.makeText(
                            MainActivity.this,
                            "No matches found in the available date range.",
                            Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String[] choices = fixtureNames.toArray(new String[0]);

                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Select a Fixture")
                        .setItems(choices, (dialog, which) -> {
                            try {
                                JSONObject selected = fixtures.get(which);

                                String homeName = selected
                                    .getJSONObject("homeTeam")
                                    .optString("name", "");

                                String awayName = selected
                                    .getJSONObject("awayTeam")
                                    .optString("name", "");

                                selectedFixture = selected;
                                homeInput.setText(homeName);
                                awayInput.setText(awayName);

                                statsSummary.setText(
                                    "RECENT TEAM FORM\nLoading recent completed matches for both teams..."
                                );
                                loadTeamStatsForFixture(selected);

                                Toast.makeText(
                                    MainActivity.this,
                                    "Fixture selected. Loading recent team statistics...",
                                    Toast.LENGTH_LONG
                                ).show();

                            } catch (Exception e) {
                                Toast.makeText(
                                    MainActivity.this,
                                    "Could not select this fixture.",
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
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
                        statsSummary.setText(
                            "RECENT TEAM FORM\nStatistics are unavailable for one or both teams. " +
                            "Please enter expected goals manually."
                        );
                        Toast.makeText(
                            MainActivity.this,
                            "Recent stats unavailable. You can enter expected goals manually.",
                            Toast.LENGTH_LONG
                        ).show();
                    });
                    return;
                }

                double homeScored = homeGoalsStats.optDouble("averageScored", 0);
                double homeConceded = homeGoalsStats.optDouble("averageConceded", 0);
                double awayScored = awayGoalsStats.optDouble("averageScored", 0);
                double awayConceded = awayGoalsStats.optDouble("averageConceded", 0);

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

                double homeVenueScored = homeAtHome != null
                    ? homeAtHome.optDouble("averageScored", homeScored) : homeScored;
                double homeVenueConceded = homeAtHome != null
                    ? homeAtHome.optDouble("averageConceded", homeConceded) : homeConceded;
                double awayVenueScored = awayAway != null
                    ? awayAway.optDouble("averageScored", awayScored) : awayScored;
                double awayVenueConceded = awayAway != null
                    ? awayAway.optDouble("averageConceded", awayConceded) : awayConceded;

                double homeAttack = homeScored * (1.0 - homeVenueWeight)
                    + homeVenueScored * homeVenueWeight;
                double homeDefence = homeConceded * (1.0 - homeVenueWeight)
                    + homeVenueConceded * homeVenueWeight;
                double awayAttack = awayScored * (1.0 - awayVenueWeight)
                    + awayVenueScored * awayVenueWeight;
                double awayDefence = awayConceded * (1.0 - awayVenueWeight)
                    + awayVenueConceded * awayVenueWeight;

                // Combine attack with the opponent's defensive concession rate.
                // Apply a modest home advantage and keep estimates within sensible bounds.
                double homeExpected = ((homeAttack + awayDefence) / 2.0) * 1.05;
                double awayExpected = (awayAttack + homeDefence) / 2.0;
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

                runOnUiThread(() -> {
                    homeGoals.setText(homeValue);
                    awayGoals.setText(awayValue);
                    statsSummary.setText(
                        "RECENT TEAM FORM (latest first)\n" +
                        homeName + ": " + homeForm + "  |  Analysed: " + homeSample + " matches\n" +
                        "Goals per match: " + homeGoalsStats.optString("averageScored", "0") +
                        " scored, " + homeGoalsStats.optString("averageConceded", "0") + " conceded\n\n" +
                        awayName + ": " + awayForm + "  |  Analysed: " + awaySample + " matches\n" +
                        "Goals per match: " + awayGoalsStats.optString("averageScored", "0") +
                        " scored, " + awayGoalsStats.optString("averageConceded", "0") + " conceded\n\n" +
                        "Venue-specific matches: " + homeName + " at home: " +
                        displayedHomeVenueSample + "; " + awayName + " away: " +
                        displayedAwayVenueSample + ". Venue rates are blended with overall recent rates, " +
                        "so small home/away samples have less influence.\n\n" +
                        "Expected goals are estimates, not guarantees. Opponent strength and league-wide " +
                        "scoring baselines are not yet included. You can edit the estimates above."
                    );
                    Toast.makeText(
                        MainActivity.this,
                        "Recent team statistics loaded.",
                        Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statsSummary.setText(
                        "RECENT TEAM FORM\nCould not load team statistics. Check your connection and try again, " +
                        "or enter expected goals manually."
                    );
                    Toast.makeText(
                        MainActivity.this,
                        "Could not load team stats. Manual entry is still available.",
                        Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
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