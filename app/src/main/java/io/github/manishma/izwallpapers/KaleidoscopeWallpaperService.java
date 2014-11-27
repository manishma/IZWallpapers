package io.github.manishma.izwallpapers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
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
        private int width;
        private int height;
        private boolean visible = true;
        private Bitmap bitmap;
        private int rotation;

        public KaleidoscopeWallpaperEngine() {
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
                this.resetBitmap();
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

            this.resetBitmap();
        }

        private void resetBitmap() {

            this.rotation = 0;
            this.bitmap = generateBitmap();

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

        private PointF getRandomPointWithinSegment(int size) {
            float x = (float) (1 + Math.random()) * size / 2;
            float y = (float) ((2 * x - size) * Math.random()) + size - x;
            return new PointF(x, y);
        }

        public Bitmap generateBitmap() {


            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            int size = 2 * (int) (160 * density / 2);

            // draw segment
            Bitmap segment = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas segmentCanvas = new Canvas(segment);
            Path path = new Path();
            path.moveTo(size / 2, size / 2);
            path.lineTo(0.933f * size, size / 4);
            path.lineTo(0.933f * size, 3 * size / 4);
            path.close();

            segmentCanvas.clipPath(path);
            segmentCanvas.drawColor(getRandomColor());

            paint.setStrokeWidth(1f * density);
            for (int i = 0; i < 5; i++) {
                Path elPath = new Path();
                PointF p = getRandomPointWithinSegment(size);
                elPath.moveTo(p.x, p.y);
                int n = 2 + (int) (3 * Math.random());
                for (int j = 0; j < n; j++) {
                    p = getRandomPointWithinSegment(size);
                    elPath.lineTo(p.x, p.y);
                }
                elPath.close();
                paint.setColor(getRandomColor());
                segmentCanvas.drawPath(elPath, paint);
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
                    canvas.drawARGB(255, 0, 0, 0);
                    int halfSize = bitmap.getWidth() / 2;//
                    Matrix matrix = new Matrix();
                    matrix.setRotate(rotation, halfSize, halfSize);
                    matrix.postTranslate(width / 2 - halfSize, height / 2 - halfSize);
                    canvas.drawBitmap(bitmap, matrix, paint);

//                    this.rotation++;

                    this.rotation = (++this.rotation) % 120;
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
//
//            if (rotation >= 60) {
//                this.resetBitmap();
//            }

            if (visible) {
                handler.postDelayed(drawRunner, 40);
            }
        }
    }
}
