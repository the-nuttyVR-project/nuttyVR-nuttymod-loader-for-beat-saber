package com.nuttyvr.common;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

/** An opt-in game: it reads a matching profile through the NuttyMod API. */
public final class DemoActivity extends Activity {
    private Playfield field;
    private TextView description;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(Ui.BG);
        int padding = Ui.dp(this, 20); page.setPadding(padding, padding, padding, padding);
        Ui.text(page, "nuttyVR / Rhythm Demo", 26, Ui.WHITE);
        description = Ui.text(page, "Loading profile…", 16, Ui.MINT);
        Ui.text(page, "Point and click a falling block. Space / Enter also hits the block in the timing zone. Reopen the game after changing profiles.", 15, Ui.MUTED);
        field = new Playfield(); page.addView(field, new LinearLayout.LayoutParams(-1, 0, 1));
        Ui.button(page, "Reset score", false, () -> field.reset());
        setContentView(page);
    }
    @Override protected void onResume() {
        super.onResume();
        try {
            ModSpec profile = NuttyModClient.activeForThisGame(this);
            field.speed = profile == null ? 1.0 : profile.speed;
            field.blockColor = profile == null ? Color.rgb(249, 167, 76) : Color.parseColor(profile.color);
            description.setText(profile == null ? "Default game · Orange blocks · Speed 1.0×" : "Mod loaded: " + profile.name + " · " + profile.color + " · Speed " + profile.speed + "×");
        } catch (Exception error) {
            field.speed = 1.0; field.blockColor = Color.rgb(249, 167, 76);
            description.setText("Default game · Runtime unavailable. Install nuttymodruntime.apk to enable profiles.");
        }
        field.running = true; field.lastFrame = SystemClock.uptimeMillis(); field.postInvalidateOnAnimation();
    }
    @Override protected void onPause() { field.running = false; super.onPause(); }
    private final class Playfield extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF block = new RectF();
        double speed = 1;
        int blockColor = Ui.MINT, score, lane;
        float progress;
        long lastFrame;
        boolean running;
        Playfield() {
            super(DemoActivity.this); setFocusable(true); setFocusableInTouchMode(true); setContentDescription("Rhythm game. Click a falling block to score, or press Enter when it reaches the timing zone.");
        }
        void reset() { score = 0; progress = 0; lane = 0; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long now = SystemClock.uptimeMillis();
            float delta = Math.min(0.05f, Math.max(0, now - lastFrame) / 1000f); lastFrame = now;
            if (running) progress += delta * 0.3f * (float) speed;
            if (progress > 1.1f) { progress = 0; lane = (lane + 1) % 3; }
            float width = getWidth(), height = getHeight();
            canvas.drawColor(Ui.PANEL);
            paint.setColor(Color.rgb(53, 68, 50)); paint.setStrokeWidth(Ui.dp(DemoActivity.this, 2));
            for (int i = 1; i < 3; i++) canvas.drawLine(width * i / 3, 0, width * i / 3, height, paint);
            paint.setColor(Ui.MUTED); canvas.drawLine(0, height * 0.8f, width, height * 0.8f, paint);
            float size = Math.min(width / 5, Ui.dp(DemoActivity.this, 88));
            float x = (lane + 0.5f) * width / 3, y = progress * height;
            block.set(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
            paint.setColor(blockColor); canvas.drawRoundRect(block, 14, 14, paint);
            paint.setColor(Ui.WHITE); paint.setTextSize(Ui.dp(DemoActivity.this, 22));
            canvas.drawText("Score  " + score, 20, Ui.dp(DemoActivity.this, 38), paint);
            if (running) postInvalidateOnAnimation();
        }
        private void hit() { score++; progress = 0; lane = (lane + 1) % 3; announceForAccessibility("Score " + score); invalidate(); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { requestFocus(); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP) { if (block.contains(event.getX(), event.getY())) hit(); performClick(); return true; }
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public boolean onKeyDown(int code, KeyEvent event) {
            if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_SPACE) {
                if (event.getRepeatCount() == 0 && progress >= 0.65f && progress <= 0.95f) hit(); return true;
            }
            return super.onKeyDown(code, event);
        }
    }
}
