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
    private TextView result, status;
    private LinearLayout mainLayout;
    private LinearLayout aiMessages;
    private EditText aiQuestion;
    private String lastAiContext = "";

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

        FrameLayout root = new FrameLayout(this);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        Button aiFab = new Button(this);
        aiFab.setText("🤖 AI");
        aiFab.setTextColor(Color.rgb(8, 25, 22));
        aiFab.setTextSize(14);
        aiFab.setTypeface(null, Typeface.BOLD);
        aiFab.setAllCaps(false);
        aiFab.setBackground(rounded(GREEN, 28));
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
            dp(82), dp(58), Gravity.BOTTOM | Gravity.END
        );
        fabParams.setMargins(dp(12), dp(12), dp(18), dp(22));
        root.addView(aiFab, fabParams);
        aiFab.setOnClickListener(v -> openAiChat());

        setContentView(root);
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
        subtitle.setPadding(0, dp(8), 0, dp(12));
        mainLayout.addView(subtitle);

        LinearLayout quickNav = new LinearLayout(this);
        quickNav.setOrientation(LinearLayout.HORIZONTAL);
        Button aiTab = new Button(this);
        aiTab.setText("💬 FOOTBALL GENIE AI");
        aiTab.setAllCaps(false);
        aiTab.setTextColor(Color.rgb(8, 25, 22));
        aiTab.setTypeface(null, Typeface.BOLD);
        aiTab.setBackground(rounded(GREEN, 14));
        quickNav.addView(aiTab, new LinearLayout.LayoutParams(0, dp(50), 1));
        aiTab.setOnClickListener(v -> openAiChat());

        Button menuButton = new Button(this);
        menuButton.setText("☰ MENU");
        menuButton.setAllCaps(false);
        menuButton.setTextColor(WHITE);
        menuButton.setBackground(rounded(Color.rgb(40, 55, 77), 14));
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(105), dp(50));
        menuParams.leftMargin = dp(8);
        quickNav.addView(menuButton, menuParams);
        menuButton.setOnClickListener(v -> showAppMenu());
        mainLayout.addView(quickNav);
        View navGap = new View(this);
        mainLayout.addView(navGap, new LinearLayout.LayoutParams(1, dp(14)));

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
            "Expected goals are model inputs, not automatically " +
            "retrieved live statistics. Use realistic estimates."
        );
        note.setTextColor(MUTED);
        note.setTextSize(12);
        note.setPadding(0, dp(8), 0, dp(10));
        form.addView(note);

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

    private void showAppMenu() {
        String[] options = {
            "🏠 Home / Predictions",
            "🤖 FOOTBALL GENIE AI",
            "🌐 Load Live Fixtures"
        };
        new AlertDialog.Builder(this)
            .setTitle("FOOTBALL GENIE MENU")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Toast.makeText(this, "You are on Home / Predictions.", Toast.LENGTH_SHORT).show();
                } else if (which == 1) {
                    openAiChat();
                } else if (which == 2) {
                    loadFixtures();
                }
            }).show();
    }

    private void openAiChat() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(14), dp(12), dp(14), dp(10));
        screen.setBackgroundColor(BG);

        TextView title = label("🤖 FOOTBALL GENIE AI");
        title.setTextColor(GREEN);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(4));
        screen.addView(title);

        TextView subtitle = label("Live AI chat • Ask, follow up, challenge the analysis");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(10));
        screen.addView(subtitle);

        ScrollView chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(false);
        aiMessages = new LinearLayout(this);
        aiMessages.setOrientation(LinearLayout.VERTICAL);
        aiMessages.setPadding(dp(4), dp(4), dp(4), dp(10));
        chatScroll.addView(aiMessages);
        screen.addView(chatScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if (aiMessages.getChildCount() == 0) {
            addChatMessage("Football Genie AI",
                "Hello! Ask me about a match, your simulation, probabilities, or why a prediction may be wrong. I will use the match details you provide and be clear about missing live statistics.", false);
        }

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        aiQuestion = new EditText(this);
        aiQuestion.setSingleLine(false);
        aiQuestion.setMinLines(1);
        aiQuestion.setMaxLines(3);
        aiQuestion.setHint("Ask Football Genie AI...");
        aiQuestion.setHintTextColor(MUTED);
        aiQuestion.setTextColor(WHITE);
        aiQuestion.setTextSize(14);
        aiQuestion.setPadding(dp(12), dp(8), dp(12), dp(8));
        aiQuestion.setBackground(rounded(Color.rgb(31, 44, 65), 12));
        inputRow.addView(aiQuestion, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button send = new Button(this);
        send.setText("SEND ➤");
        send.setAllCaps(false);
        send.setTextColor(Color.rgb(8, 25, 22));
        send.setTypeface(null, Typeface.BOLD);
        send.setBackground(rounded(GREEN, 12));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(94), dp(54));
        sendParams.leftMargin = dp(8);
        inputRow.addView(send, sendParams);
        send.setOnClickListener(v -> sendAiQuestion());
        aiQuestion.setOnEditorActionListener((v, actionId, event) -> {
            sendAiQuestion();
            return true;
        });
        screen.addView(inputRow);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button home = new Button(this);
        home.setText("🏠 HOME");
        home.setAllCaps(false);
        home.setTextColor(WHITE);
        home.setBackground(rounded(Color.rgb(40, 55, 77), 12));
        nav.addView(home, new LinearLayout.LayoutParams(0, dp(48), 1));
        home.setOnClickListener(v -> {
            recreate();
        });
        Button menu = new Button(this);
        menu.setText("☰ MENU");
        menu.setAllCaps(false);
        menu.setTextColor(WHITE);
        menu.setBackground(rounded(Color.rgb(40, 55, 77), 12));
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        menuParams.leftMargin = dp(8);
        nav.addView(menu, menuParams);
        menu.setOnClickListener(v -> showAppMenu());
        screen.addView(nav, new LinearLayout.LayoutParams(-1, dp(52)));

        setContentView(screen);
    }

    private void addChatMessage(String sender, String message, boolean fromAi) {
        if (aiMessages == null) return;
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        bubble.setBackground(rounded(fromAi ? Color.rgb(25, 48, 62) : Color.rgb(37, 61, 49), 14));
        TextView who = label(sender);
        who.setTextColor(fromAi ? GREEN : Color.rgb(170, 255, 200));
        who.setTextSize(12);
        who.setTypeface(null, Typeface.BOLD);
        bubble.addView(who);
        TextView body = label(message);
        body.setTextColor(WHITE);
        body.setTextSize(14);
        body.setPadding(0, dp(5), 0, 0);
        bubble.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(9);
        aiMessages.addView(bubble, params);
    }

    private void sendAiQuestion() {
        if (aiQuestion == null || aiMessages == null) return;
        String question = aiQuestion.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(this, "Type a question first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (question.length() > 1200) {
            Toast.makeText(this, "Please keep the question under 1,200 characters.", Toast.LENGTH_LONG).show();
            return;
        }

        String home = homeInput == null ? "" : homeInput.getText().toString().trim();
        String away = awayInput == null ? "" : awayInput.getText().toString().trim();
        String hx = homeGoals == null ? "" : homeGoals.getText().toString().trim();
        String ax = awayGoals == null ? "" : awayGoals.getText().toString().trim();
        addChatMessage("YOU", question, false);
        aiQuestion.setText("");
        aiQuestion.setEnabled(false);

        TextView waiting = label("Football Genie AI is thinking...");
        waiting.setTextColor(GREEN);
        waiting.setTextSize(13);
        aiMessages.addView(waiting);
        JSONObject payload = new JSONObject();
        JSONObject match = new JSONObject();
        try {
            payload.put("question", question);
            match.put("home", home.isEmpty() ? "Home team (not supplied)" : home);
            match.put("away", away.isEmpty() ? "Away team (not supplied)" : away);
            if (!hx.isEmpty()) match.put("homeXg", Double.parseDouble(hx.replace(',', '.')));
            if (!ax.isEmpty()) match.put("awayXg", Double.parseDouble(ax.replace(',', '.')));
            payload.put("match", match);
        } catch (Exception ignored) { }

        new Thread(() -> {
            HttpURLConnection connection = null;
            String answer = null;
            String error = null;
            try {
                URL url = new URL("https://football-genie-api.onrender.com/api/ai/analyze");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] bytes = payload.toString().getBytes("UTF-8");
                connection.getOutputStream().write(bytes);
                int code = connection.getResponseCode();
                java.io.InputStream stream = code >= 200 && code < 400
                    ? connection.getInputStream() : connection.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();
                JSONObject response = new JSONObject(body.toString());
                if (code >= 200 && code < 300) {
                    answer = response.optString("answer", "The AI returned no answer.");
                    String provider = response.optString("provider", "");
                    if (!provider.isEmpty()) answer += "\n\n— Powered by " + provider + " (" + response.optString("model", "AI model") + ")";
                } else {
                    error = response.optString("error", "AI request failed (HTTP " + code + ").");
                }
            } catch (Exception ex) {
                error = "Could not reach Football Genie AI. Check your internet connection and try again.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String finalAnswer = answer;
            final String finalError = error;
            runOnUiThread(() -> {
                if (waiting.getParent() != null) aiMessages.removeView(waiting);
                aiQuestion.setEnabled(true);
                if (finalAnswer != null) addChatMessage("FOOTBALL GENIE AI", finalAnswer, true);
                else addChatMessage("AI CONNECTION", finalError == null ? "Unknown error. Please try again." : finalError, true);
            });
        }).start();
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
                "This version uses only the expected-goals " +
                "values you entered. It has not checked live " +
                "fixtures, recent form, injuries, line-ups, " +
                "weather or real team statistics. Those factors " +
                "may change the real match outcome.";

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
        private void loadFixtures() {
        Toast.makeText(
            MainActivity.this,
            "Loading live fixtures...",
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

                    String status = match.optString("status", "SCHEDULED");

if (!status.equals("SCHEDULED")
&& !status.equals("TIMED")) {
continue;
}

String kickoff = match.optString("utcDate", "");

try {
java.text.SimpleDateFormat dateFormat =
new java.text.SimpleDateFormat(
"yyyy-MM-dd'T'HH:mm:ss'Z'",
java.util.Locale.US
);

dateFormat.setTimeZone(
    java.util.TimeZone.getTimeZone("UTC")
);

java.util.Date kickoffDate = dateFormat.parse(kickoff);

if (kickoffDate != null
        && kickoffDate.getTime() <= System.currentTimeMillis()) {
    continue;
}

} catch (java.text.ParseException e) {
continue;
}

                    JSONObject home = match.optJSONObject("homeTeam");
                    JSONObject away = match.optJSONObject("awayTeam");

                    if (home == null || away == null) {
                        continue;
                    }

                    String homeName = home.optString("name", "Home team");
                    String awayName = away.optString("name", "Away team");

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
                        display += "\nKickoff: "
                            + date.substring(0, 10)
                            + " "
                            + date.substring(11, 16)
                            + " UTC";
                    }

                    display += "\nStatus: Upcoming";

                    fixtures.add(match);
                    fixtureNames.add(display);
                }

                

                runOnUiThread(() -> {
                    if (fixtures.isEmpty()) {
                        Toast.makeText(
                            MainActivity.this,
                            "No fixtures found. Please try again later.",
                            Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String[] choices = fixtureNames.toArray(new String[0]);

                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Select a Live Fixture")
                        .setItems(choices, (dialog, which) -> {
                            try {
                                JSONObject selected = fixtures.get(which);

                                String homeName = selected
                                    .getJSONObject("homeTeam")
                                    .optString("name", "");

                                String awayName = selected
                                    .getJSONObject("awayTeam")
                                    .optString("name", "");

                                homeInput.setText(homeName);
                                awayInput.setText(awayName);

                                Toast.makeText(
                                    MainActivity.this,
                                    "Fixture selected. Enter expected goals to simulate.",
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
    }
