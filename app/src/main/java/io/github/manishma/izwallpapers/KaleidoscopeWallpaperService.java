package io.github.manishma.izwallpapers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
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

        private final float density;
        private final int size;
        private final float patternWidthF;
        private final float patternHeightF;
        private final int patternWidth;
        private final int patternHeight;
        private final Path segmentPath;
        private final Path patternPath;
        private int width;
        private int height;
        private boolean visible = true;
        private int bgColor;
        private Strip strip;
        private int columns;
        private int lines;
        private int y0;
        private int x0;

        private class Strip {

            private int stampsOffset;
            private Bitmap[] stamps;
            private Bitmap bitmap;

            public Strip() {

                stamps = new Bitmap[]{
                        generateStamp(),
                        generateStamp(),
                        generateStamp()
                };

                bitmap = generateStrip();

            }

            public Bitmap getBitmap() {
                return bitmap;
            }

            public int getStampsOffset() {
                return stampsOffset;
            }

            public void recycle() {

                for (int i = 0; i < stamps.length; i++) {
                    stamps[i].recycle();
                }
                bitmap.recycle();

            }

            public void move() {

                stampsOffset += (int) density;
                if (stampsOffset >= size / 2) {
                    stampsOffset = 0;
                    stamps[0].recycle();
                    for (int i = 0; i < stamps.length - 1; i++) {
                        stamps[i] = stamps[i + 1];
                    }
                    stamps[stamps.length - 1] = generateStamp();
                    bitmap = generateStrip();
                }

            }

            private PointF getRandomPointWithinStamp() {
                float x = (float) (size * Math.random() / 2);
                float y = (float) (size * Math.random());
                return new PointF(x, y);
            }

            private Bitmap generateStrip() {
                Bitmap bitmap = Bitmap.createBitmap(size / 2, size * 2, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                Matrix matrix = new Matrix();

                canvas.drawColor(bgColor);
                for (int i = 0; i < stamps.length; i++) {
                    matrix.reset();
                    matrix.postTranslate(0, size - i * size / 2);
                    canvas.drawBitmap(stamps[i], matrix, null);
                }

                return bitmap;
            }

            private Bitmap generateStamp() {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);

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

                for (int i = 0; i < 2; i++) {
                    PointF p = getRandomPointWithinStamp();
                    paint.setColor(getRandomColor());
                    canvas.drawCircle(p.x, p.y, (float) ((Math.random() * size) / 4), paint);
                }

                return bitmap;
            }

        }

        public KaleidoscopeWallpaperEngine() {
            density = getResources().getDisplayMetrics().density;

            size = 4 * (int) (160 * density / 4);

            patternWidthF = size * 0.866f;
            patternHeightF = size;

            patternWidth = 2 * (int) Math.ceil(patternWidthF / 2);
            patternHeight = size;

            segmentPath = new Path();
            segmentPath.moveTo(0, 0);
            segmentPath.lineTo(0.433f * size, -size / 4);
            segmentPath.lineTo(0.433f * size, size / 4);
            segmentPath.close();

            patternPath = new Path();
            patternPath.moveTo(-0.433f * size, -size / 4);
            patternPath.lineTo(0, -size / 2);
            patternPath.lineTo(0.433f * size, -size / 4);
            patternPath.lineTo(0.433f * size, size / 4);
            patternPath.lineTo(0, size / 2);
            patternPath.lineTo(-0.433f * size, size / 4);
            patternPath.close();

            bgColor = getRandomColor();
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

            holder.setFormat(PixelFormat.RGB_565);
            strip = new Strip();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            this.width = width;
            this.height = height;

            this.columns = (int) Math.ceil((this.width - patternWidthF) / 2 / patternWidthF);
            this.lines = (int) Math.ceil((2 * this.height - patternHeightF) / 3 / patternHeightF);
            this.y0 = this.height / 2;
            this.x0 = this.width / 2;

        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);

            this.strip.recycle();
            this.strip = null;

            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        private int getRandomColor() {
            float hue = (float) (360f * Math.random());
            float sat = (float) (.5f * Math.random() + .2f);
            return Color.HSVToColor(new float[]{hue, sat, .9f});
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {

                    canvas.drawColor(bgColor);

                    Matrix matrix = new Matrix();

                    Bitmap pattern = Bitmap.createBitmap(patternWidth, patternHeight, Bitmap.Config.ARGB_8888);
                    Canvas pattenCanvas = new Canvas(pattern);
                    for (int r = 0; r < 6; r++) {

                        matrix.reset();
                        matrix.postRotate(60 * r, 0, 0);
                        matrix.postTranslate(patternWidth / 2, patternHeight / 2);

                        Path path = new Path(segmentPath);
                        path.transform(matrix);

                        pattenCanvas.save();
                        pattenCanvas.clipPath(path);

                        // draw strip
                        matrix.reset();
                        matrix.postTranslate(0, 0 - 5 * size / 4 + strip.getStampsOffset());
                        if (r % 2 > 0) {
                            matrix.postScale(1, -1);
                        }
                        matrix.postRotate(60 * r, 0, 0);
                        matrix.postTranslate(patternWidth / 2, patternHeight / 2);
                        pattenCanvas.drawBitmap(strip.getBitmap(), matrix, null);

                        pattenCanvas.restore();
                    }


                    for (int l = -lines; l <= lines; l++) {

                        float y = y0 + l * 3 * patternHeightF / 4;

                        for (int c = -columns; c <= columns + Math.abs(l) % 2; c++) {

                            float x = x0 + c * patternWidthF - (Math.abs(l) % 2) * patternWidthF / 2;

                            matrix.reset();
                            matrix.postTranslate(x, y);
                            Path path = new Path(patternPath);
                            path.transform(matrix);

                            canvas.save();
                            canvas.clipPath(path);

                            // draw pattern
                            matrix.reset();
                            matrix.postTranslate(x - patternWidth / 2, y - patternHeight / 2);
                            canvas.drawBitmap(pattern, matrix, null);

                            canvas.restore();

                        }
                    }

                    pattern.recycle();
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }

            this.strip.move();

            if (visible) {
                handler.postDelayed(drawRunner, 40);
            }
        }
    }
}
