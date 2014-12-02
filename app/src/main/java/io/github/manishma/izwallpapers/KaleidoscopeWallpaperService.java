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
            density = getResources().getDisplayMetrics().density;

            size = 4 * (int) (160 * density / 4);

            segmentPath = new Path();
            segmentPath.moveTo(0, 0);
            segmentPath.lineTo(0.433f * size, -size / 4);
            segmentPath.lineTo(0.433f * size, size / 4);
            segmentPath.close();

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

            stamps = new Bitmap[]{
                    generateStamp(),
                    generateStamp(),
                    generateStamp()
            };

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

            for (int i = 0; i < stamps.length; i++) {
                stamps[i].recycle();
            }
            stamps = null;
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        private int getRandomColor() {
            float hue = (float) (360f * Math.random());
            return Color.HSVToColor(new float[]{hue, 1f, 1f});
        }

        private PointF getRandomPointWithinStamp() {
            float x = (float) (size * Math.random() / 2);
            float y = (float) (size * Math.random());
            return new PointF(x, y);
        }

        public Bitmap generateStamp() {
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

            for(int i = 0; i<2; i++) {
                PointF p = getRandomPointWithinStamp();
                paint.setColor(getRandomColor());
                canvas.drawCircle(p.x, p.y, (float) ((Math.random() * size) / 4), paint);
            }

            return bitmap;
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {

                    canvas.drawColor(bgColor);

                    float patternWidth = size * 0.866f;
                    float patternHeight = size;

                    int columns = (int) Math.ceil((this.width - patternWidth) / 2 / patternWidth);
                    int lines = (int) Math.ceil((2 * this.height - patternHeight) / 3 / patternHeight);
                    float y0 = this.height / 2;
                    float x0 = this.width / 2;
                    Matrix matrix = new Matrix();

                    for (int l = -lines; l <= lines; l++) {

                        float y = y0 + l * 3 * patternHeight / 4;

                        for (int c = -columns; c <= columns + Math.abs(l) % 2; c++) {

                            float x = x0 + c * patternWidth - (Math.abs(l) % 2) * patternWidth / 2;

                            for (int r = 0; r < 6; r++) {

                                matrix.reset();
                                matrix.postRotate(60 * r, 0, 0);
                                matrix.postTranslate(x, y);

                                Path path = new Path(segmentPath);
                                path.transform(matrix);

                                canvas.save();
                                canvas.clipPath(path);

                                for (int i = 0; i < stamps.length; i++) {
                                    matrix.reset();
                                    matrix.postTranslate(0, 0 - size / 4 + stampsOffset - i * size / 2);
                                    if (r % 2 > 0) {
                                        matrix.postScale(1, -1);
                                    }
                                    matrix.postRotate(60 * r, 0, 0);
                                    matrix.postTranslate(x, y);
                                    canvas.drawBitmap(stamps[i], matrix, null);
                                }

                                canvas.restore();
                            }

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
                stamps[0].recycle();
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
