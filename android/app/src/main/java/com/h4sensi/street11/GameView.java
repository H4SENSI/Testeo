package com.h4sensi.street11;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    private static final int MENU = 0;
    private static final int MATCH = 1;
    private static final int PAUSE = 2;
    private static final int RESULT = 3;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final List<Player> players = new ArrayList<>();
    private final Random random = new Random(11);

    private int state = MENU;
    private float screenW;
    private float screenH;
    private long lastFrame;
    private float menuPulse;
    private float matchClock;
    private int homeScore;
    private int awayScore;
    private int selectedPlayer = 7;
    private boolean joystickActive;
    private float joystickX;
    private float joystickY;
    private float joystickDX;
    private float joystickDY;
    private boolean initialized;
    private float goalFlash;
    private String resultTitle = "";

    private final Ball ball = new Ball();

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        p.setStrokeCap(Paint.Cap.ROUND);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(3f);
        text.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        lastFrame = SystemClock.uptimeMillis();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        screenW = w;
        screenH = h;
        initialized = true;
        if (state == MATCH) {
            resetMatch();
        }
    }

    private float sx() {
        return screenW;
    }

    private float sy() {
        return screenH;
    }

    private float left() {
        return 34f;
    }

    private float right() {
        return screenW - 34f;
    }

    private float top() {
        return 78f;
    }

    private float bottom() {
        return screenH - 36f;
    }

    private float midY() {
        return (top() + bottom()) * 0.5f;
    }

    private float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    private float len(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    private void startMatch() {
        state = MATCH;
        matchClock = 0f;
        homeScore = 0;
        awayScore = 0;
        goalFlash = 0f;
        resultTitle = "";
        resetMatch();
    }

    private void resetMatch() {
        players.clear();

        String[] names = {
                "MENDOZA", "REYES", "SOSA", "ACOSTA", "FERRER",
                "VEGA", "LOPEZ", "RAMOS", "TORRES", "DIAZ", "BENITEZ"
        };

        float pitchW = right() - left();
        float pitchH = bottom() - top();

        // Home team, attacking to the right.
        float[][] home = {
                {0.08f, 0.50f}, {0.18f, 0.25f}, {0.18f, 0.75f},
                {0.32f, 0.38f}, {0.32f, 0.62f}, {0.47f, 0.24f},
                {0.47f, 0.50f}, {0.47f, 0.76f}, {0.67f, 0.32f},
                {0.67f, 0.68f}, {0.82f, 0.50f}
        };

        for (int i = 0; i < home.length; i++) {
            float x = left() + pitchW * home[i][0];
            float y = top() + pitchH * home[i][1];
            players.add(new Player(x, y, x, y, true, i + 1, names[i]));
        }

        // Away team, with slightly varied positioning.
        for (int i = 0; i < home.length; i++) {
            float x = left() + pitchW * (1f - home[i][0]);
            float y = top() + pitchH * home[i][1];
            players.add(new Player(x, y, x, y, false, i + 1, "CPU"));
        }

        selectedPlayer = 7;
        for (int i = 0; i < players.size(); i++) {
            players.get(i).controlled = (i == selectedPlayer - 1);
        }

        ball.x = screenW * 0.5f;
        ball.y = midY();
        ball.vx = 0f;
        ball.vy = 0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!initialized) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.033f, (now - lastFrame) / 1000f);
        lastFrame = now;

        if (state == MATCH) {
            update(dt);
            drawMatch(canvas);
        } else if (state == PAUSE) {
            drawMatch(canvas);
            drawPause(canvas);
        } else if (state == RESULT) {
            drawResult(canvas);
        } else {
            updateMenu(dt);
            drawMenu(canvas);
        }

        postInvalidateOnAnimation();
    }

    private void updateMenu(float dt) {
        menuPulse += dt;
        if (menuPulse > 1000f) {
            menuPulse = 0;
        }
    }

    private void update(float dt) {
        if (goalFlash > 0f) {
            goalFlash -= dt;
        }

        matchClock += dt;
        if (matchClock >= 90f) {
            matchClock = 90f;
            if (homeScore > awayScore) {
                resultTitle = "VICTORY";
            } else if (homeScore < awayScore) {
                resultTitle = "DEFEAT";
            } else {
                resultTitle = "DRAW";
            }
            state = RESULT;
            return;
        }

        Player controlled = getControlledPlayer();
        if (controlled != null) {
            float speed = 245f;
            if (joystickActive) {
                controlled.vx = joystickDX * speed;
                controlled.vy = joystickDY * speed;
            } else {
                controlled.vx *= 0.75f;
                controlled.vy *= 0.75f;
            }
        }

        float l = left() + 14f;
        float rr = right() - 14f;
        float tt = top() + 14f;
        float bb = bottom() - 14f;

        for (Player pl : players) {
            if (!pl.home || !pl.controlled) {
                updateCpuPlayer(pl, dt);
            }

            pl.x += pl.vx * dt;
            pl.y += pl.vy * dt;
            pl.vx *= 0.86f;
            pl.vy *= 0.86f;

            pl.x = clamp(pl.x, l, rr);
            pl.y = clamp(pl.y, tt, bb);
        }

        handlePlayerBallInteraction(dt);
        updateBall(dt);
    }

    private Player getControlledPlayer() {
        for (Player pl : players) {
            if (pl.home && pl.controlled) return pl;
        }
        return null;
    }

    private void updateCpuPlayer(Player pl, float dt) {
        float targetX = pl.homeX;
        float targetY = pl.homeY;

        if (!pl.home) {
            float distance = len(ball.x - pl.x, ball.y - pl.y);
            if (distance < 260f || isNearestOpponent(pl)) {
                targetX = ball.x;
                targetY = ball.y;
            } else {
                targetX += (ball.x - screenW * 0.5f) * 0.10f;
                targetY += (ball.y - midY()) * 0.08f;
            }
        } else {
            float dBall = len(ball.x - pl.x, ball.y - pl.y);
            if (dBall < 155f && !pl.keeper) {
                targetX = ball.x - 22f;
                targetY = ball.y;
            } else {
                targetX += (ball.x - screenW * 0.5f) * 0.055f;
                targetY += (ball.y - midY()) * 0.055f;
            }
        }

        float dx = targetX - pl.x;
        float dy = targetY - pl.y;
        float d = len(dx, dy);
        if (d > 3f) {
            float speed = pl.home ? 115f : 145f;
            pl.vx += (dx / d) * speed * dt * 6f;
            pl.vy += (dy / d) * speed * dt * 6f;
            float max = speed;
            float v = len(pl.vx, pl.vy);
            if (v > max) {
                pl.vx = pl.vx / v * max;
                pl.vy = pl.vy / v * max;
            }
        }
    }

    private boolean isNearestOpponent(Player pl) {
        float d = len(ball.x - pl.x, ball.y - pl.y);
        for (Player other : players) {
            if (other.home) continue;
            if (other == pl) continue;
            if (len(ball.x - other.x, ball.y - other.y) < d) return false;
        }
        return true;
    }

    private void handlePlayerBallInteraction(float dt) {
        for (Player pl : players) {
            float dx = ball.x - pl.x;
            float dy = ball.y - pl.y;
            float d = len(dx, dy);

            if (d < pl.radius + ball.radius + 11f) {
                if (pl.controlled && joystickActive && len(joystickDX, joystickDY) > 0.12f) {
                    float push = 55f;
                    ball.vx += joystickDX * push * dt * 8f;
                    ball.vy += joystickDY * push * dt * 8f;
                    ball.ownerHome = true;
                } else if (!pl.home && d < 28f) {
                    float aimX = left() + (right() - left()) * 0.94f;
                    float aimY = midY();
                    float ax = aimX - ball.x;
                    float ay = aimY - ball.y;
                    float ad = Math.max(1f, len(ax, ay));
                    ball.vx = ax / ad * 205f;
                    ball.vy = ay / ad * 205f;
                    ball.ownerHome = false;
                }
            }
        }
    }

    private void updateBall(float dt) {
        ball.x += ball.vx * dt;
        ball.y += ball.vy * dt;

        float friction = (float) Math.pow(0.986, dt * 60f);
        ball.vx *= friction;
        ball.vy *= friction;

        float goalTop = midY() - 57f;
        float goalBottom = midY() + 57f;

        if (ball.x < left() - 28f) {
            if (ball.y >= goalTop && ball.y <= goalBottom) {
                awayScore++;
                goalFlash = 1.8f;
                resetAfterGoal();
                return;
            }
            ball.x = left() + 2f;
            ball.vx = Math.abs(ball.vx) * 0.65f;
        }

        if (ball.x > right() + 28f) {
            if (ball.y >= goalTop && ball.y <= goalBottom) {
                homeScore++;
                goalFlash = 1.8f;
                resetAfterGoal();
                return;
            }
            ball.x = right() - 2f;
            ball.vx = -Math.abs(ball.vx) * 0.65f;
        }

        if (ball.y < top() + 2f) {
            ball.y = top() + 2f;
            ball.vy = Math.abs(ball.vy) * 0.76f;
        } else if (ball.y > bottom() - 2f) {
            ball.y = bottom() - 2f;
            ball.vy = -Math.abs(ball.vy) * 0.76f;
        }
    }

    private void resetAfterGoal() {
        for (Player pl : players) {
            pl.x = pl.homeX;
            pl.y = pl.homeY;
            pl.vx = 0f;
            pl.vy = 0f;
        }
        ball.x = screenW * 0.5f;
        ball.y = midY();
        ball.vx = (homeScore + awayScore) % 2 == 0 ? -65f : 65f;
        ball.vy = 0f;
    }

    private void drawBase(Canvas c) {
        c.drawColor(Color.rgb(5, 8, 12));

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(10, 17, 25));
        c.drawRect(0, 0, screenW, screenH, p);

        p.setColor(Color.argb(35, 70, 210, 255));
        c.drawCircle(screenW * 0.15f, screenH * 0.05f, screenW * 0.24f, p);
        p.setColor(Color.argb(25, 102, 255, 176));
        c.drawCircle(screenW * 0.9f, screenH * 0.84f, screenW * 0.28f, p);
    }

    private void drawMatch(Canvas c) {
        drawBase(c);
        drawPitch(c);
        drawPlayers(c);
        drawBall(c);
        drawHud(c);
        drawControls(c);

        if (goalFlash > 0f) {
            float a = clamp(goalFlash / 1.8f, 0f, 1f);
            p.setColor(Color.argb((int)(a * 42), 255, 255, 255));
            p.setStyle(Paint.Style.FILL);
            c.drawRect(0, 0, screenW, screenH, p);
            drawCentered(c, "GOAL", screenW * 0.5f, screenH * 0.35f, 48f, Color.WHITE, true);
        }
    }

    private void drawPitch(Canvas c) {
        float l = left();
        float rr = right();
        float t = top();
        float b = bottom();

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(16, 113, 72));
        r.set(l, t, rr, b);
        c.drawRoundRect(r, 20f, 20f, p);

        float stripeW = (rr - l) / 10f;
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                p.setColor(Color.argb(22, 255, 255, 255));
                c.drawRect(l + i * stripeW, t, l + (i + 1) * stripeW, b, p);
            }
        }

        line.setColor(Color.argb(225, 240, 248, 245));
        line.setStrokeWidth(3f);
        line.setStyle(Paint.Style.STROKE);

        r.set(l, t, rr, b);
        c.drawRoundRect(r, 20f, 20f, line);
        c.drawLine(screenW * 0.5f, t, screenW * 0.5f, b, line);
        c.drawCircle(screenW * 0.5f, midY(), 68f, line);
        c.drawCircle(screenW * 0.5f, midY(), 4f, line);

        float boxW = (rr - l) * 0.14f;
        float boxH = (b - t) * 0.48f;
        r.set(l, midY() - boxH * 0.5f, l + boxW, midY() + boxH * 0.5f);
        c.drawRect(r, line);
        r.set(rr - boxW, midY() - boxH * 0.5f, rr, midY() + boxH * 0.5f);
        c.drawRect(r, line);

        float smallW = (rr - l) * 0.055f;
        float smallH = (b - t) * 0.25f;
        r.set(l, midY() - smallH * 0.5f, l + smallW, midY() + smallH * 0.5f);
        c.drawRect(r, line);
        r.set(rr - smallW, midY() - smallH * 0.5f, rr, midY() + smallH * 0.5f);
        c.drawRect(r, line);

        p.setColor(Color.rgb(220, 224, 218));
        p.setStyle(Paint.Style.FILL);
        r.set(l - 28f, midY() - 59f, l + 3f, midY() + 59f);
        c.drawRoundRect(r, 8f, 8f, p);
        r.set(rr - 3f, midY() - 59f, rr + 28f, midY() + 59f);
        c.drawRoundRect(r, 8f, 8f, p);
    }

    private void drawPlayers(Canvas c) {
        for (Player pl : players) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(55, 0, 0, 0));
            c.drawCircle(pl.x + 2f, pl.y + 5f, pl.radius + 3f, p);

            if (pl.controlled) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                p.setColor(Color.WHITE);
                c.drawCircle(pl.x, pl.y, pl.radius + 8f, p);
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(pl.home ? Color.rgb(30, 125, 255) : Color.rgb(234, 66, 84));
            c.drawCircle(pl.x, pl.y, pl.radius + 1f, p);

            p.setColor(pl.home ? Color.rgb(174, 220, 255) : Color.rgb(255, 180, 185));
            c.drawCircle(pl.x, pl.y - 3f, pl.radius * 0.48f, p);

            drawCentered(c, String.valueOf(pl.number), pl.x, pl.y + 4f, 11f,
                    Color.WHITE, true);

            if (pl.controlled) {
                drawCentered(c, "YOU", pl.x, pl.y - 31f, 9f, Color.WHITE, true);
            }
        }
    }

    private void drawBall(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(75, 0, 0, 0));
        c.drawCircle(ball.x + 2f, ball.y + 3f, ball.radius + 3f, p);
        p.setColor(Color.WHITE);
        c.drawCircle(ball.x, ball.y, ball.radius, p);
        p.setColor(Color.rgb(35, 39, 44));
        c.drawCircle(ball.x, ball.y, 2.2f, p);
        c.drawCircle(ball.x + 3.2f, ball.y - 2.4f, 1.6f, p);
        c.drawCircle(ball.x - 3.3f, ball.y + 2.4f, 1.6f, p);
    }

    private void drawHud(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(235, 6, 10, 15));
        r.set(18, 14, screenW - 18, 66);
        c.drawRoundRect(r, 14f, 14f, p);

        drawText(c, "STREET 11", 34, 46, 12f, Color.rgb(155, 240, 194), true);
        drawText(c, String.format(Locale.US, "%02d:%02d", (int)matchClock / 60,
                (int)matchClock % 60), screenW - 82, 46, 13f, Color.WHITE, true);

        drawCentered(c, homeScore + "  -  " + awayScore, screenW * 0.5f, 47, 24f,
                Color.WHITE, true);

        p.setColor(Color.argb(80, 130, 210, 255));
        c.drawRoundRect(new RectF(18, 67, screenW - 18, 69), 2, 2, p);

        drawText(c, "BLUE UNITED", screenW * 0.5f - 120f, 34f, 9f,
                Color.rgb(150, 194, 255), true);
        drawText(c, "RED ATHLETIC", screenW * 0.5f + 44f, 34f, 9f,
                Color.rgb(255, 158, 169), true);

        // Pause button.
        p.setColor(Color.argb(210, 20, 26, 34));
        r.set(screenW - 62, 18, screenW - 28, 52);
        c.drawRoundRect(r, 10, 10, p);
        drawText(c, "Ⅱ", screenW - 52, 41, 16f, Color.WHITE, true);
    }

    private void drawControls(Canvas c) {
        float baseX = 105f;
        float baseY = screenH - 112f;

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(62, 255, 255, 255));
        c.drawCircle(baseX, baseY, 72f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(Color.argb(80, 255, 255, 255));
        c.drawCircle(baseX, baseY, 72f, p);

        float knobX = baseX + joystickDX * 38f;
        float knobY = baseY + joystickDY * 38f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(190, 255, 255, 255));
        c.drawCircle(knobX, knobY, 27f, p);

        drawActionButton(c, screenW - 98f, screenH - 102f, 50f, "TIRO", Color.rgb(255, 77, 94));
        drawActionButton(c, screenW - 215f, screenH - 160f, 43f, "PASE", Color.rgb(48, 154, 255));

        drawText(c, "MOVER", 72f, screenH - 28f, 10f, Color.argb(180, 255,255,255), true);
    }

    private void drawActionButton(Canvas c, float x, float y, float radius, String label, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(80, 0, 0, 0));
        c.drawCircle(x + 2f, y + 4f, radius + 2f, p);

        p.setColor(color);
        c.drawCircle(x, y, radius, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(Color.argb(160, 255, 255, 255));
        c.drawCircle(x, y, radius, p);

        drawCentered(c, label, x, y + 4f, label.equals("PASE") ? 10f : 11f,
                Color.WHITE, true);
    }

    private void drawMenu(Canvas c) {
        drawBase(c);

        // Decorative pitch lines.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(Color.argb(32, 107, 216, 255));
        for (int i = 0; i < 8; i++) {
            float x = screenW * 0.56f + i * 56f;
            c.drawLine(x, 0, x - 190f, screenH, p);
        }

        drawText(c, "SEASON // 01", 58, 72, 12f, Color.rgb(113, 190, 255), true);
        drawText(c, "STREET 11", 58, 148, 58f, Color.WHITE, true);
        drawText(c, "MOBILE FOOTBALL", 61, 178, 15f, Color.rgb(154, 236, 201), true);
        drawText(c, "OFFLINE  •  QUICK MATCH", 61, 205, 11f, Color.rgb(139, 151, 164), true);

        // Main card.
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(230, 11, 18, 26));
        r.set(58, 238, screenW * 0.56f, screenH - 46f);
        c.drawRoundRect(r, 22f, 22f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(Color.argb(60, 102, 197, 255));
        c.drawRoundRect(r, 22f, 22f, p);

        drawText(c, "MATCH CENTER", 84, 278, 11f, Color.rgb(137, 154, 173), true);
        drawText(c, "BLUE UNITED", 84, 316, 24f, Color.WHITE, true);
        drawText(c, "OVR  82", 84, 340, 11f, Color.rgb(120, 194, 255), true);

        drawActionButton(c, screenW * 0.47f, 330, 36f, "VS", Color.rgb(37, 52, 71));

        drawText(c, "RED ATHLETIC", 84, 386, 24f, Color.WHITE, true);
        drawText(c, "OVR  81", 84, 410, 11f, Color.rgb(255, 133, 145), true);

        // Play button.
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(56, 168, 255));
        r.set(84, 448, screenW * 0.56f - 24, 506);
        c.drawRoundRect(r, 15, 15, p);
        drawCentered(c, "PLAY QUICK MATCH", (84 + screenW * 0.56f - 24) * 0.5f,
                484, 15f, Color.WHITE, true);

        drawText(c, "2 x 45 SECONDS", 84, 541, 11f, Color.rgb(137, 154, 173), true);
        drawText(c, "HAPTICS READY", 220, 541, 11f, Color.rgb(92, 218, 160), true);

        // Right status panel.
        float panelL = screenW * 0.62f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(218, 10, 16, 23));
        r.set(panelL, 82, screenW - 58, screenH - 82);
        c.drawRoundRect(r, 20f, 20f, p);

        drawText(c, "PLAYER PROFILE", panelL + 28, 122, 11f, Color.rgb(137, 154, 173), true);
        drawText(c, "H4SENSI FC", panelL + 28, 164, 30f, Color.WHITE, true);

        drawStat(c, panelL + 30, 205, "OVR", "82");
        drawStat(c, panelL + 155, 205, "W", String.valueOf(homeScore));
        drawStat(c, panelL + 30, 286, "COINS", "12,540");
        drawStat(c, panelL + 155, 286, "FORM", "+6");

        p.setColor(Color.argb(35, 99, 255, 183));
        r.set(panelL + 28, 362, screenW - 86, 434);
        c.drawRoundRect(r, 14, 14, p);
        drawText(c, "BUILD", panelL + 46, 389, 10f, Color.rgb(145, 171, 187), true);
        drawText(c, "PACE  •  PRESS  •  FINISH", panelL + 46, 415, 12f,
                Color.rgb(104, 221, 177), true);

        drawText(c, "NO LOGIN  •  NO ADS", panelL + 28, screenH - 112, 10f,
                Color.rgb(114, 129, 145), true);
        drawText(c, "v1.0  /  READY", panelL + 28, screenH - 88, 10f,
                Color.rgb(92, 218, 160), true);
    }

    private void drawStat(Canvas c, float x, float y, String label, String value) {
        drawText(c, label, x, y, 9f, Color.rgb(112, 129, 145), true);
        drawText(c, value, x, y + 31, 23f, Color.WHITE, true);
    }

    private void drawPause(Canvas c) {
        p.setColor(Color.argb(150, 3, 6, 9));
        p.setStyle(Paint.Style.FILL);
        c.drawRect(0, 0, screenW, screenH, p);

        p.setColor(Color.rgb(12, 19, 27));
        r.set(screenW * 0.33f, screenH * 0.2f, screenW * 0.67f, screenH * 0.8f);
        c.drawRoundRect(r, 22f, 22f, p);

        drawCentered(c, "MATCH PAUSED", screenW * 0.5f, screenH * 0.34f, 27f, Color.WHITE, true);
        drawCentered(c, "The grass is still. Humanity survives.", screenW * 0.5f,
                screenH * 0.43f, 11f, Color.rgb(131, 148, 163), false);

        drawPauseButton(c, screenW * 0.5f, screenH * 0.55f, "RESUME");
        drawPauseButton(c, screenW * 0.5f, screenH * 0.67f, "QUIT");
    }

    private void drawPauseButton(Canvas c, float x, float y, String label) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(label.equals("RESUME") ? Color.rgb(53, 166, 255) : Color.rgb(45, 55, 68));
        r.set(x - 115, y - 25, x + 115, y + 25);
        c.drawRoundRect(r, 12, 12, p);
        drawCentered(c, label, x, y + 4, 12f, Color.WHITE, true);
    }

    private void drawResult(Canvas c) {
        drawBase(c);

        drawText(c, "FULL TIME", 0, 0, 1f, Color.WHITE, false);
        drawCentered(c, resultTitle, screenW * 0.5f, screenH * 0.26f, 48f,
                resultTitle.equals("VICTORY") ? Color.rgb(98, 230, 165) : Color.WHITE, true);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(235, 11, 18, 26));
        r.set(screenW * 0.28f, screenH * 0.37f, screenW * 0.72f, screenH * 0.66f);
        c.drawRoundRect(r, 24f, 24f, p);

        drawCentered(c, "BLUE UNITED", screenW * 0.38f, screenH * 0.46f, 15f,
                Color.rgb(145, 196, 255), true);
        drawCentered(c, "RED ATHLETIC", screenW * 0.62f, screenH * 0.46f, 15f,
                Color.rgb(255, 151, 163), true);
        drawCentered(c, String.valueOf(homeScore), screenW * 0.38f, screenH * 0.56f, 50f,
                Color.WHITE, true);
        drawCentered(c, String.valueOf(awayScore), screenW * 0.62f, screenH * 0.56f, 50f,
                Color.WHITE, true);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(53, 166, 255));
        r.set(screenW * 0.35f, screenH * 0.73f, screenW * 0.65f, screenH * 0.82f);
        c.drawRoundRect(r, 15, 15, p);
        drawCentered(c, "PLAY AGAIN", screenW * 0.5f, screenH * 0.785f, 14f, Color.WHITE, true);
    }

    private void drawText(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
        text.setTextSize(size);
        text.setColor(color);
        text.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setTextAlign(Paint.Align.LEFT);
        c.drawText(s, x, y, text);
    }

    private void drawCentered(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
        text.setTextSize(size);
        text.setColor(color);
        text.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, x, y, text);
    }

    private void shoot() {
        if (state != MATCH) return;

        Player pl = getControlledPlayer();
        if (pl == null) return;

        float dx = ball.x - pl.x;
        float dy = ball.y - pl.y;
        float d = len(dx, dy);

        if (d < 70f) {
            float aimX = right() + 55f;
            float aimY = midY() + (ball.y - midY()) * 0.25f;
            float ax = aimX - ball.x;
            float ay = aimY - ball.y;
            float ad = Math.max(1f, len(ax, ay));
            float power = 535f;
            ball.vx = ax / ad * power;
            ball.vy = ay / ad * power;
            ball.ownerHome = true;
        }
    }

    private void pass() {
        if (state != MATCH) return;

        Player pl = getControlledPlayer();
        if (pl == null) return;

        float best = Float.MAX_VALUE;
        Player target = null;
        for (Player candidate : players) {
            if (!candidate.home || candidate == pl) continue;
            if (candidate == getKeeper()) continue;

            float dx = candidate.x - pl.x;
            float dy = candidate.y - pl.y;
            float d = len(dx, dy);

            // Favor players upfield.
            float forwardPenalty = candidate.x < pl.x ? 260f : 0f;
            float score = d + forwardPenalty;
            if (score < best) {
                best = score;
                target = candidate;
            }
        }

        float ballDist = len(ball.x - pl.x, ball.y - pl.y);
        if (target != null && ballDist < 78f) {
            float ax = target.x - ball.x;
            float ay = target.y - ball.y;
            float ad = Math.max(1f, len(ax, ay));
            ball.vx = ax / ad * 350f;
            ball.vy = ay / ad * 350f;
            ball.ownerHome = true;
        }
    }

    private Player getKeeper() {
        for (Player pl : players) {
            if (pl.home && pl.keeper) return pl;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) return true;

        float x = event.getX();
        float y = event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (state == MENU) {
                if (x >= 58f && x <= screenW * 0.56f && y >= 448f && y <= 520f) {
                    startMatch();
                    performClick();
                    return true;
                }
            } else if (state == MATCH) {
                if (x > screenW - 82f && y < 74f) {
                    state = PAUSE;
                    performClick();
                    return true;
                }

                if (distance(x, y, screenW - 98f, screenH - 102f) < 70f) {
                    shoot();
                    performClick();
                    return true;
                }

                if (distance(x, y, screenW - 215f, screenH - 160f) < 60f) {
                    pass();
                    performClick();
                    return true;
                }

                if (x < screenW * 0.45f && y > screenH * 0.52f) {
                    joystickActive = true;
                    updateJoystick(x, y);
                    return true;
                }
            } else if (state == PAUSE) {
                float cx = screenW * 0.5f;
                if (Math.abs(x - cx) < 150f && Math.abs(y - screenH * 0.55f) < 35f) {
                    state = MATCH;
                    performClick();
                    return true;
                }
                if (Math.abs(x - cx) < 150f && Math.abs(y - screenH * 0.67f) < 35f) {
                    state = MENU;
                    performClick();
                    return true;
                }
            } else if (state == RESULT) {
                if (x >= screenW * 0.35f && x <= screenW * 0.65f
                        && y >= screenH * 0.71f && y <= screenH * 0.84f) {
                    startMatch();
                    performClick();
                    return true;
                }
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (state == MATCH && joystickActive) {
                updateJoystick(x, y);
                return true;
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            joystickActive = false;
            joystickDX = 0f;
            joystickDY = 0f;
            return true;
        }

        return true;
    }

    private void updateJoystick(float x, float y) {
        float baseX = 105f;
        float baseY = screenH - 112f;
        float dx = x - baseX;
        float dy = y - baseY;
        float d = len(dx, dy);
        if (d > 72f) {
            dx = dx / d * 72f;
            dy = dy / d * 72f;
        }
        joystickX = dx;
        joystickY = dy;
        joystickDX = dx / 72f;
        joystickDY = dy / 72f;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return len(x1 - x2, y1 - y2);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static class Player {
        float x;
        float y;
        float homeX;
        float homeY;
        float vx;
        float vy;
        final boolean home;
        final int number;
        final String name;
        boolean controlled;
        boolean keeper;
        final float radius = 16f;

        Player(float x, float y, float homeX, float homeY, boolean home, int number, String name) {
            this.x = x;
            this.y = y;
            this.homeX = homeX;
            this.homeY = homeY;
            this.home = home;
            this.number = number;
            this.name = name;
            this.keeper = number == 1;
        }
    }

    private static class Ball {
        float x;
        float y;
        float vx;
        float vy;
        float radius = 8f;
        boolean ownerHome;
    }
}
