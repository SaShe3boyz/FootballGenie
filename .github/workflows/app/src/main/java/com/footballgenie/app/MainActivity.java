package com.footballgenie.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    EditText homeInput, awayInput, homeGoals, awayGoals;
    TextView result;
    Button simulateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(12, 24, 39));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("FOOTBALL GENIE");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 12);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("PREDICT • SIMULATE • DISCOVER");
        subtitle.setTextColor(Color.rgb(100, 220, 170));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 24);
        layout.addView(subtitle);

        homeInput = addInput(layout, "Home team");
        awayInput = addInput(layout, "Away team");
        homeGoals = addInput(layout, "Expected home goals (e.g. 1.6)");
        awayGoals = addInput(layout, "Expected away goals (e.g. 1.1)");

        simulateButton = new Button(this);
        simulateButton.setText("SIMULATE 10,000 MATCHES");
        layout.addView(simulateButton);

        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(16);
        result.setPadding(0, 24, 0, 24);
        layout.addView(result);

        simulateButton.setOnClickListener(v -> simulate());

        setContentView(scroll);
    }

    private EditText addInput(LinearLayout layout, String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(Color.LTGRAY);
        input.setTextColor(Color.WHITE);
        input.setInputType(
            hint.contains("goals")
                ? 8194
                : 1
        );
        input.setPadding(12, 12, 12, 12);
        layout.addView(input);
        return input;
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
            String home = homeInput.getText().toString().trim();
            String away = awayInput.getText().toString().trim();

            double homeXg = Double.parseDouble(
                homeGoals.getText().toString().trim()
            );
            double awayXg = Double.parseDouble(
                awayGoals.getText().toString().trim()
            );

            if (home.isEmpty() || away.isEmpty()
                    || homeXg < 0 || awayXg < 0
                    || homeXg > 8 || awayXg > 8) {
                throw new IllegalArgumentException();
            }

            int homeWins = 0, draws = 0, awayWins = 0;
            int over25 = 0, btts = 0;
            int[][] scores = new int[9][9];
            Random random = new Random();

            for (int i = 0; i < 10000; i++) {
                int hg = (int) poisson(random, homeXg);
                int ag = (int) poisson(random, awayXg);

                if (hg > ag) homeWins++;
                else if (hg == ag) draws++;
                else awayWins++;

                if (hg + ag > 2) over25++;
                if (hg > 0 && ag > 0) btts++;

                if (hg < 9 && ag < 9) scores[hg][ag]++;
            }

            int bestHome = 0, bestAway = 0, bestCount = 0;

            for (int h = 0; h < 9; h++) {
                for (int a = 0; a < 9; a++) {
                    if (scores[h][a] > bestCount) {
                        bestCount = scores[h][a];
                        bestHome = h;
                        bestAway = a;
                    }
                }
            }

            result.setText(String.format(Locale.US,
                "SIMULATION RESULTS (10,000 RUNS)\n\n" +
                "%s win: %.1f%%\n" +
                "Draw: %.1f%%\n" +
                "%s win: %.1f%%\n\n" +
                "Over 2.5 goals: %.1f%%\n" +
                "Under 2.5 goals: %.1f%%\n" +
                "Both teams to score: %.1f%%\n\n" +
                "Most frequent score: %s %d - %d %s\n\n" +
                "Expected goals entered:\n%s: %.2f\n%s: %.2f\n\n" +
                "These are model-based estimates, not guarantees.",
                home, homeWins / 100.0,
                draws / 100.0,
                away, awayWins / 100.0,
                over25 / 100.0,
                (10000 - over25) / 100.0,
                btts / 100.0,
                home, bestHome, bestAway, away,
                home, homeXg, away, awayXg
            ));

        } catch (Exception e) {
            Toast.makeText(this,
                "Enter both team names and valid expected goals (0–8).",
                Toast.LENGTH_LONG).show();
        }
    }
}
