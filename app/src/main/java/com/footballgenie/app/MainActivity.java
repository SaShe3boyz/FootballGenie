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
            for (int h = 0; h < rankedScoreProbabilities.length; h++) {
                for (int a = 0; a < rankedScoreProbabilities[h].length; a++) {
                    rankedScoreProbabilities[h][a] =
                        poissonProbability(h, homeXg) * poissonProbability(a, awayXg)
                        * lowScoreCorrection(h, a, homeXg, awayXg);
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
                        rankProbability * 100.0)).append("%)");
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

                "3. Confidence assessment\n" + confidence + "\n" +
                confidenceReason + "\n" +
                "This rating reflects simulation probabilities only; it is not a guarantee of accuracy.\n\n" +

                "4. Goals analysis\n" + goalTrend +
                "\n" + bttsTrend +
                "\n\n" +

                "5. Most frequent score\n" +
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
                "MODEL CONFIDENCE\n" +
                "%s\n\n" +
                "GOALS MARKETS\n" +
                "Over 2.5 goals       %.1f%%\n" +
                "Under 2.5 goals     %.1f%%\n" +
                "Both teams score    %.1f%%\n\n" +
                "MOST FREQUENT SCORE\n" +
                "%s %d - %d %s\n\n" +
                "TOP 3 SCORELINES (simulation frequency)\n%s\n\n%s\n\n%s",
                home, homeP,
                drawP,
                away, awayP,
                confidence,
                overP, underP, bttsP,
                home, bestHome, bestAway, away,
                topScores.toString(),
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

    private double square(double value) {
        return value * value;
    }

    private void showPredictionHistory(int newlyUpdated) {
        JSONArray history = readPredictionHistory();
        int finished = 0, outcomeCorrect = 0, goalsCorrect = 0;
        int bttsCorrect = 0, exactCorrect = 0;
        double outcomeBrierTotal = 0.0;
        double goalsBrierTotal = 0.0;
        double bttsBrierTotal = 0.0;
        StringBuilder summary = new StringBuilder();
        summary.append("PREDICTION HISTORY\n");
        summary.append("New final results added: ").append(newlyUpdated).append("\n\n");

        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject record = history.optJSONObject(i);
            if (record == null) continue;
            String home = record.optString("home", "Home");
            String away = record.optString("away", "Away");
            String state = record.optString("predictionStatus", "PENDING");
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

            if ("FINISHED".equals(state)) {
                int ah = record.optInt("actualHome", -1);
                int aa = record.optInt("actualAway", -1);
                if (ah >= 0 && aa >= 0) {
                    finished++;
                    String predictedOutcome = record.optDouble("homeP", 0) >=
                        Math.max(record.optDouble("drawP", 0), record.optDouble("awayP", 0))
                        ? "HOME" : (record.optDouble("drawP", 0) >= record.optDouble("awayP", 0)
                            ? "DRAW" : "AWAY");
                    boolean actualHome = ah > aa;
                    boolean actualDraw = ah == aa;
                    boolean actualAway = ah < aa;
                    String actualOutcome = actualHome ? "HOME" : (actualDraw ? "DRAW" : "AWAY");
                    if (predictedOutcome.equals(actualOutcome)) outcomeCorrect++;

                    double hp = record.optDouble("homeP", 0) / 100.0;
                    double dp = record.optDouble("drawP", 0) / 100.0;
                    double ap = record.optDouble("awayP", 0) / 100.0;
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
                "Outcome pick correct: " + outcomeCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * outcomeCorrect / finished) + "%)\n" +
                "Over/Under pick correct: " + goalsCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * goalsCorrect / finished) + "%)\n" +
                "BTTS pick correct: " + bttsCorrect + "/" + finished +
                " (" + String.format(Locale.US, "%.1f", 100.0 * bttsCorrect / finished) + "%)\n" +
                "Exact score: " + exactCorrect + "/" + finished + "\n\n" +
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

                // First estimate from recent form and venue performance.
                double homeExpected = ((homeAttack + awayDefence) / 2.0) * 1.05;
                double awayExpected = (awayAttack + homeDefence) / 2.0;

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

                // Partly normalize recent goal rates for the strength of opponents faced.
                // This is a conservative adjustment using current season venue rates;
                // it falls back to unadjusted form when opponent IDs/stats are missing.
                boolean usedOpponentAdjustment = false;
                if (usedCompetitionStrength) {
                    double adjustedHomeScored = opponentAdjustedRecentAverage(
                        homeRecentMatches, true, competitionStrength, competitionId
                    );
                    double adjustedHomeConceded = opponentAdjustedRecentAverage(
                        homeRecentMatches, false, competitionStrength, competitionId
                    );
                    double adjustedAwayScored = opponentAdjustedRecentAverage(
                        awayRecentMatches, true, competitionStrength, competitionId
                    );
                    double adjustedAwayConceded = opponentAdjustedRecentAverage(
                        awayRecentMatches, false, competitionStrength, competitionId
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
                final String homeWeightedScoredText = String.format(Locale.US, "%.2f", homeScored);
                final String homeWeightedConcededText = String.format(Locale.US, "%.2f", homeConceded);
                final String awayWeightedScoredText = String.format(Locale.US, "%.2f", awayScored);
                final String awayWeightedConcededText = String.format(Locale.US, "%.2f", awayConceded);

                runOnUiThread(() -> {
                    homeGoals.setText(homeValue);
                    awayGoals.setText(awayValue);
                    statsSummary.setText(
                        "RECENT TEAM FORM (latest first)\n" +
                        homeName + ": " + homeForm + "  |  Analysed: " + homeSample + " matches\n" +
                        "Overall goals per match: " + homeGoalsStats.optString("averageScored", "0") +
                        " scored, " + homeGoalsStats.optString("averageConceded", "0") + " conceded\n" +
                        "Recency-weighted form: " + homeWeightedScoredText +
                        " scored, " + homeWeightedConcededText + " conceded per match\n" +
                        "Estimated match xG: " + homeValue + "\n\n" +
                        awayName + ": " + awayForm + "  |  Analysed: " + awaySample + " matches\n" +
                        "Overall goals per match: " + awayGoalsStats.optString("averageScored", "0") +
                        " scored, " + awayGoalsStats.optString("averageConceded", "0") + " conceded\n" +
                        "Recency-weighted form: " + awayWeightedScoredText +
                        " scored, " + awayWeightedConcededText + " conceded per match\n" +
                        "Estimated match xG: " + awayValue + "\n\n" +
                        "Venue-specific matches: " + homeName + " at home: " +
                        displayedHomeVenueSample + "; " + awayName + " away: " +
                        displayedAwayVenueSample + ". Venue rates are blended with overall recent rates, " +
                        "so small home/away samples have less influence.\n\n" +
                        "Model method: recent form and venue rates are blended with the opponent's " +
                        "defensive record and a modest home advantage. " +
                        (usedCompetitionStrength
                            ? "Current competition scoring baselines and season home/away strength are also included. "
                            : "Competition standings were unavailable, so season-strength adjustment was skipped. ") +
                        (appliedOpponentAdjustment
                            ? "Recent form was also adjusted using available opponent strengths from this competition. "
                            : "No usable same-competition opponent ratings were available for recent-form adjustment. ") +
                        "These remain estimates; check team news and edit expected goals if needed."
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

    // Normalize recent goals against the current venue-specific strength of opponents.
    // A 50% blend is applied by the caller to limit overreaction to noisy standings.
    private double opponentAdjustedRecentAverage(
            JSONArray matches, boolean forGoals, JSONObject competitionStrength,
            String targetCompetitionId) {
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