package io.github.manishma.izwallpapers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;


public class KaleidoscopeWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new KaleidoscopeWallpaperEngine();
    }

    private class KaleidoscopeWallpaperEngine extends Engine {

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }

        };

        private final Paint paint;
        private final float density;
        private final int size;
        private final Path segmentPath;
        private int width;
        private int height;
        private boolean visible = true;
        private Bitmap[] stamps;
        private int stampsOffset;
        private int bgColor;

        public KaleidoscopeWallpaperEngine() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            density = getResources().getDisplayMetrics().density;

            size = 4 * (int) (160 * density / 4);

            segmentPath = new Path();
            segmentPath.moveTo(size / 2, size / 2);
            segmentPath.lineTo(0.933f * size, size / 4);
            segmentPath.lineTo(0.933f * size, 3 * size / 4);
            segmentPath.close();

            bgColor = getRandomColor();

            stamps = new Bitmap[]{
                    generateStamp(),
                    generateStamp(),
                    generateStamp()
            };
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
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        private int getRandomColor() {
            float hue = (float) (360f * Math.random());
            return Color.HSVToColor(new float[]{hue, 1, 0.5f});
        }

        private PointF getRandomPointWithinStamp() {
            float x = (float) (size * Math.random() / 2);
            float y = (float) (size * Math.random());
            return new PointF(x, y);
        }

        public Bitmap generateStamp() {

            Bitmap bitmap = Bitmap.createBitmap(size / 2, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            for (int i = 0; i < 3; i++) {
                Path elPath = new Path();
                PointF p = getRandomPointWithinStamp();
                elPath.moveTo(p.x, p.y);
                int n = 2 + (int) (3 * Math.random());
                for (int j = 0; j < n; j++) {
                    p = getRandomPointWithinStamp();
                    elPath.lineTo(p.x, p.y);
                }
                elPath.close();
                paint.setColor(getRandomColor());
                canvas.drawPath(elPath, paint);
            }

            return bitmap;
        }

        public Bitmap generatePattern() {

            // draw segment
            Bitmap segment = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas segmentCanvas = new Canvas(segment);
            segmentCanvas.clipPath(segmentPath);

            for (int i = 0; i < stamps.length; i++) {
                segmentCanvas.drawBitmap(stamps[i], size / 2, size / 4 + stampsOffset - i * size / 2, paint);
            }

            Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            for (int i = 0; i < 6; i++) {
                Matrix matrix = new Matrix();
                if (i % 2 == 0) {
                    matrix.setRotate(60 * i, size / 2, size / 2);
                } else {
                    matrix.setScale(1, -1);
                    matrix.postTranslate(0, size);
                    matrix.postRotate(60 * i, size / 2, size / 2);
                }
                canvas.drawBitmap(segment, matrix, paint);
            }

            return bm;
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {

                    canvas.drawColor(bgColor);

                    Bitmap pattern = generatePattern();

                    float patternWidth = size * 0.866f;
                    float patternHeight = size;

                    int columns = (int) Math.ceil((this.width - patternWidth) / 2 / patternWidth);
                    int lines = (int) Math.ceil((2 * this.height - patternHeight) / 3 / patternHeight);
                    float y0 = this.height / 2 - size / 2;
                    float x0 = this.width / 2 - size / 2;

                    for (int l = -lines; l <= lines; l++) {

                        float y = y0 + l * 3 * patternHeight / 4;

                        for (int c = -columns; c <= columns + Math.abs(l) % 2; c++) {

                            float x = x0 + c * patternWidth - (Math.abs(l) % 2) * patternWidth / 2;
                            Matrix matrix = new Matrix();
                            matrix.postTranslate(x, y);
                            canvas.drawBitmap(pattern, matrix, paint);
                        }
                    }
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }

            stampsOffset += (int) density;
            if (stampsOffset >= size / 2) {
                stampsOffset = 0;
                for (int i = 0; i < stamps.length - 1; i++) {
                    stamps[i] = stamps[i + 1];
                }
                stamps[stamps.length - 1] = generateStamp();
            }

            if (visible) {
                handler.postDelayed(drawRunner, 40);
            }
        }
    }
}
