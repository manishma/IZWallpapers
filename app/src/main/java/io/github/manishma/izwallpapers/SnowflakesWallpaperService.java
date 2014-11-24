package io.github.manishma.izwallpapers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

public class SnowflakesWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new SnowflakesWallpaperEngine();
    }

    private static class Snowflake {
        private final float hSpeed;
        private final float rSpeed;
        private Point position;
        private float density;
        private Bitmap bitmap;

        public Snowflake(Point position, float density) {
            this.position = position;
            this.density = density;
            this.hSpeed = (float) (0.5f - Math.random()) * 0.7f;
            this.rSpeed = (float) (0.5f - Math.random());
            this.bitmap = createBitmap(this.density);
        }

        private static Bitmap createBitmap(float density) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            int size = (int) ((40 + 20 * Math.random()) * density);

            // draw segment
            Bitmap segment = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas segmentCanvas = new Canvas(segment);
            Path path = new Path();
            path.moveTo(size / 2, size / 2);
            path.lineTo(0.933f * size, size / 4);
            path.lineTo(size, size / 2);
            path.close();

            segmentCanvas.clipPath(path);

            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(2f * density);
            segmentCanvas.drawLine(size / 2, size / 2, size, size / 2, paint);

            paint.setStrokeWidth(1f * density);
            for (int i = 0; i < 5; i++) {
                segmentCanvas.drawLine((float) (1 + Math.random()) * size / 2, size / 2, (float) (1 + Math.random()) * size / 2, (float) (Math.random()) * size / 2, paint);
            }


            Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            for (int i = 0; i < 6; i++) {
                Matrix matrix = new Matrix();
                matrix.setRotate(60 * i, size / 2, size / 2);
                canvas.drawBitmap(segment, matrix, paint);

                matrix = new Matrix();
                matrix.setScale(1, -1);
                matrix.postTranslate(0, size);
                matrix.postRotate(60 * i, size / 2, size / 2);
                canvas.drawBitmap(segment, matrix, paint);
            }

            return bm;
        }

        public Point getPosition() {
            return position;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public float getHSpeed() {
            return hSpeed;
        }

        public float getRSpeed() {
            return rSpeed;
        }
    }

    private class SnowflakesWallpaperEngine extends Engine {
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }

        };

        private final Paint paint;
        private final float density;
        int hue = 0;
        private boolean visible = true;
        private final List<Snowflake> dots = new ArrayList<Snowflake>();
        private int width;
        private int height;

        public SnowflakesWallpaperEngine() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            density = getResources().getDisplayMetrics().density;

            handler.post(drawRunner);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            this.width = width;
            this.height = height;

            // init points
            dots.clear();
            for (int i = 0; i < 10; i++) {
                Point position = new Point((int) (this.width * Math.random()), (int) (this.height * Math.random()));
                dots.add(new Snowflake(position, density));
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    paint.setColor(Color.HSVToColor(new float[]{this.hue, 1, 0.5f}));
                    this.hue = (++this.hue) % 360;
                    canvas.drawRect(0, 0, this.width, this.height, paint);

                    // draw dots
                    for (int i = 0; i < dots.size(); i++) {
                        Snowflake flake = dots.get(i);
                        Point p = flake.getPosition();
                        Bitmap bitmap = flake.getBitmap();

                        Matrix matrix = new Matrix();
                        matrix.setRotate(((int) (5 * this.hue * flake.getRSpeed())) % 360, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                        matrix.postTranslate(p.x - bitmap.getWidth() / 2, p.y - bitmap.getHeight() / 2);
                        canvas.drawBitmap(flake.getBitmap(), matrix, paint);

                        p.offset((int) (density * 3 * (0.5f - Math.random() + flake.getHSpeed())), (int) (1.5 * density));

                        if (p.y > height || p.x < 0 || p.x > width) {
                            dots.set(i, new Snowflake(new Point((int) (this.width * Math.random()), 0), density));
                        }
                    }
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
            if (visible) {
                handler.postDelayed(drawRunner, 40);
            }
        }
    }
}