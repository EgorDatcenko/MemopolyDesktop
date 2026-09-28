package com.memopoly.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Декодер GIF-файлов для LibGDX.
 * Парсит кадры GIF, прозрачность, методы утилизации (disposal methods)
 * и длительность задержек кадров (Graphic Control Extension).
 */
public class GifDecoder {

    public static class GifResult {
        public Animation<TextureRegion> animation;
        public List<Texture> textures = new ArrayList<>();
    }

    public static Animation<TextureRegion> loadGIFAnimation(Animation.PlayMode playMode, FileHandle file) {
        GifResult result = loadGIFResult(playMode, file);
        return result != null ? result.animation : null;
    }

    public static Animation<TextureRegion> loadGIFAnimation(Animation.PlayMode playMode, InputStream is) {
        GifResult result = loadGIFResult(playMode, is);
        return result != null ? result.animation : null;
    }

    public static GifResult loadGIFResult(Animation.PlayMode playMode, FileHandle file) {
        if (file == null || !file.exists())
            return null;
        try (InputStream is = file.read()) {
            return loadGIFResult(playMode, is);
        } catch (Exception e) {
            com.badlogic.gdx.Gdx.app.error("GifDecoder", "Failed to load GIF file: " + file.path(), e);
            return null;
        }
    }

    public static GifResult loadGIFResult(Animation.PlayMode playMode, InputStream is) {
        if (is == null)
            return null;
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            GifDecoderInternal decoder = new GifDecoderInternal();
            decoder.read(bis);
            return decoder.buildAnimation(playMode);
        } catch (Exception e) {
            com.badlogic.gdx.Gdx.app.error("GifDecoder", "Error decoding GIF stream", e);
            return null;
        }
    }

    private static class GifFrame {
        Pixmap pixmap;
        int delay; // ms
        int disposal;
        int ix, iy, iw, ih;
    }

    private static class GifDecoderInternal {
        private static final int MAX_STACK_SIZE = 4096;

        private InputStream in;
        private int status;
        private int width;
        private int height;
        private boolean gctFlag;
        private int gctSize;
        private int loopCount = 1;

        private int[] gct;
        private int[] lct;
        private int[] act;

        private int bgIndex;
        private int bgColor;
        private int pixelAspect;

        private boolean lctFlag;
        private int lctSize;

        private int ix, iy, iw, ih;
        private int lctSizeVal;

        private int dispose = 0;
        private int lastDispose = 0;
        private boolean transparency = false;
        private int delay = 0;
        private int transIndex;

        private short[] prefix;
        private byte[] suffix;
        private byte[] pixelStack;
        private byte[] pixels;

        private List<GifFrame> frames = new ArrayList<>();

        public void read(InputStream is) throws Exception {
            in = is;
            readHeader();
            if (!err()) {
                readContents();
            }
        }

        private boolean err() {
            return status != 0;
        }

        private void readHeader() throws Exception {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append((char) readByte());
            }
            String id = sb.toString();
            if (!id.startsWith("GIF")) {
                status = 1;
                return;
            }
            readLSD();
            if (gctFlag && !err()) {
                gct = readColorTable(gctSize);
                if (bgIndex >= 0 && bgIndex < gct.length) {
                    bgColor = gct[bgIndex];
                }
            }
        }

        private void readLSD() throws Exception {
            width = readShort();
            height = readShort();
            int packed = readByte();
            gctFlag = (packed & 0x80) != 0;
            gctSize = 2 << (packed & 7);
            bgIndex = readByte();
            pixelAspect = readByte();
        }

        private void readContents() throws Exception {
            boolean done = false;
            while (!done && !err()) {
                int code = readByte();
                switch (code) {
                    case 0x2C: // Image Separator
                        readImage();
                        break;
                    case 0x21: // Extension
                        code = readByte();
                        switch (code) {
                            case 0xF9: // Graphic Control Extension
                                readGraphicControlExt();
                                break;
                            case 0xFF: // Application Extension (e.g. NETSCAPE2.0 loop block)
                                // readBlock() already reads all sub-blocks up to the zero terminator.
                                // A second skip() would consume the next stream byte as a block size,
                                // corrupting the parser position and leaving frames empty.
                                readBlock();
                                break;
                            default:
                                skip();
                        }
                        break;
                    case 0x3B: // Terminator
                        done = true;
                        break;
                    case 0x00: // Bad byte
                        break;
                    default:
                        status = 1;
                        break;
                }
            }
        }

        private void readGraphicControlExt() throws Exception {
            readByte(); // block size
            int packed = readByte();
            dispose = (packed & 0x1C) >> 2;
            if (dispose == 0)
                dispose = 1;
            transparency = (packed & 1) != 0;
            delay = readShort() * 10; // convert hundredths of a second to ms
            transIndex = readByte();
            readByte(); // block terminator
        }

        private void readImage() throws Exception {
            ix = readShort();
            iy = readShort();
            iw = readShort();
            ih = readShort();

            int packed = readByte();
            lctFlag = (packed & 0x80) != 0;
            boolean interlace = (packed & 0x40) != 0;
            lctSize = 2 << (packed & 7);

            if (lctFlag) {
                lct = readColorTable(lctSize);
                act = lct;
            } else {
                act = gct;
            }

            if (act == null) {
                status = 1;
                return;
            }

            int save = 0;
            boolean transValid = transparency && transIndex >= 0 && transIndex < act.length;
            if (transValid) {
                save = act[transIndex];
                act[transIndex] = 0;
            }

            decodeImageData();
            skip();

            if (err()) {
                if (transValid) {
                    act[transIndex] = save;
                }
                return;
            }

            // Build Pixmap for this frame
            Pixmap framePixmap = new Pixmap(iw, ih, Pixmap.Format.RGBA8888);
            framePixmap.setBlending(Pixmap.Blending.None);

            // Build interlace line-order map: GIF89a spec — 4 passes
            int[] imap = new int[ih];
            if (interlace) {
                int idx = 0;
                // Pass 1: every 8th row starting at 0
                for (int row = 0; row < ih; row += 8) imap[idx++] = row;
                // Pass 2: every 8th row starting at 4
                for (int row = 4; row < ih; row += 8) imap[idx++] = row;
                // Pass 3: every 4th row starting at 2
                for (int row = 2; row < ih; row += 4) imap[idx++] = row;
                // Pass 4: every 2nd row starting at 1
                for (int row = 1; row < ih; row += 2) imap[idx++] = row;
            } else {
                for (int row = 0; row < ih; row++) imap[row] = row;
            }

            for (int i = 0; i < ih; i++) {
                int line = imap[i];
                if (line < ih) {
                    int k = i * iw;
                    for (int dx = 0; dx < iw; dx++) {
                        int c = pixels[k++] & 0xff;
                        if (transparency && c == transIndex) {
                            framePixmap.drawPixel(dx, line, 0x00000000);
                        } else {
                            int col = (c < act.length) ? act[c] : 0;
                            framePixmap.drawPixel(dx, line, col);
                        }
                    }
                }
            }

            if (transValid) {
                act[transIndex] = save;
            }

            GifFrame frame = new GifFrame();
            frame.pixmap = framePixmap;
            frame.delay = delay <= 0 ? 100 : delay;
            frame.disposal = dispose;
            frame.ix = ix;
            frame.iy = iy;
            frame.iw = iw;
            frame.ih = ih;
            frames.add(frame);

            resetFrame();
        }

        private void resetFrame() {
            lastDispose = dispose;
            dispose = 0;
            transparency = false;
            delay = 0;
        }

        private void decodeImageData() throws Exception {
            int NullCode = -1;
            int npix = iw * ih;

            if (pixels == null || pixels.length < npix) {
                pixels = new byte[npix];
            }
            if (prefix == null)
                prefix = new short[MAX_STACK_SIZE];
            if (suffix == null)
                suffix = new byte[MAX_STACK_SIZE];
            if (pixelStack == null)
                pixelStack = new byte[MAX_STACK_SIZE + 1];

            int data_size = readByte();
            int clear = 1 << data_size;
            int eoi = clear + 1;
            int available = clear + 2;
            int old_code = NullCode;
            int code_size = data_size + 1;
            int code_mask = (1 << code_size) - 1;

            for (int code = 0; code < clear; code++) {
                prefix[code] = 0;
                suffix[code] = (byte) code;
            }

            int datum = 0;
            int bits = 0;
            int count = 0;
            int first = 0;
            int top = 0;
            int bi = 0;
            int pi = 0;

            byte[] block = new byte[256];

            for (int i = 0; i < npix;) {
                if (top == 0) {
                    if (bits < code_size) {
                        if (count == 0) {
                            count = readBlock(block);
                            if (count <= 0)
                                break;
                            bi = 0;
                        }
                        datum += (((int) block[bi]) & 0xff) << bits;
                        bits += 8;
                        bi++;
                        count--;
                        continue;
                    }

                    int code = datum & code_mask;
                    datum >>= code_size;
                    bits -= code_size;

                    if (code == eoi)
                        break;

                    if (code > available)
                        break;

                    if (code == clear) {
                        code_size = data_size + 1;
                        code_mask = (1 << code_size) - 1;
                        available = clear + 2;
                        old_code = NullCode;
                        continue;
                    }

                    if (old_code == NullCode) {
                        pixelStack[top++] = suffix[code];
                        old_code = code;
                        first = code;
                        continue;
                    }

                    int in_code = code;
                    if (code == available) {
                        pixelStack[top++] = (byte) first;
                        code = old_code;
                    }

                    while (code > clear) {
                        pixelStack[top++] = suffix[code];
                        code = prefix[code];
                    }

                    first = ((int) suffix[code]) & 0xff;
                    // Always push the decoded symbol — do NOT break the loop here.
                    // GIF89a spec: when the dictionary is full, stop adding entries
                    // but continue decoding with existing codes until Clear Code or EOI.
                    pixelStack[top++] = (byte) first;

                    if (available < MAX_STACK_SIZE) {
                        prefix[available] = (short) old_code;
                        suffix[available] = (byte) first;
                        available++;

                        if (((available & code_mask) == 0) && (available < MAX_STACK_SIZE)) {
                            code_size++;
                            code_mask = (1 << code_size) - 1;
                        }
                    }
                    old_code = in_code;
                }

                top--;
                pixels[pi++] = pixelStack[top];
                i++;
            }

            for (int i = pi; i < npix; i++) {
                pixels[i] = 0;
            }
        }

        private int readByte() throws Exception {
            return in.read() & 0xff;
        }

        private int readShort() throws Exception {
            return readByte() | (readByte() << 8);
        }

        private int[] readColorTable(int ncolors) throws Exception {
            int nbytes = 3 * ncolors;
            byte[] c = new byte[nbytes];
            int n = 0;
            while (n < nbytes) {
                int r = in.read(c, n, nbytes - n);
                if (r < 0)
                    break;
                n += r;
            }
            int[] tab = new int[256];
            int i = 0;
            int j = 0;
            while (i < ncolors) {
                int r = ((int) c[j++]) & 0xff;
                int g = ((int) c[j++]) & 0xff;
                int b = ((int) c[j++]) & 0xff;
                tab[i++] = (r << 24) | (g << 16) | (b << 8) | 0xff;
            }
            return tab;
        }

        private int readBlock(byte[] block) throws Exception {
            int blockSize = readByte();
            int n = 0;
            if (blockSize > 0) {
                int count = 0;
                while (n < blockSize) {
                    count = in.read(block, n, blockSize - n);
                    if (count < 0)
                        break;
                    n += count;
                }
            }
            return n;
        }

        private void readBlock() throws Exception {
            int blockSize = readByte();
            while (blockSize > 0) {
                skip(blockSize);
                blockSize = readByte();
            }
        }

        private void skip(int count) throws Exception {
            for (int i = 0; i < count; i++) {
                in.read();
            }
        }

        private void skip() throws Exception {
            readBlock();
        }

        public GifResult buildAnimation(Animation.PlayMode playMode) {
            if (frames.isEmpty())
                return null;

            GifResult result = new GifResult();
            Array<TextureRegion> regions = new Array<>();

            // Composite canvas for handling GIF Disposal Methods
            Pixmap canvas = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            canvas.setBlending(Pixmap.Blending.None);
            canvas.setColor(0, 0, 0, 0);
            canvas.fill();

            Pixmap prevCanvas = null;
            // Среднее время кадра по всем кадрам, минимум 50мс
            float totalDelay = 0f;
            for (GifFrame fr : frames) totalDelay += fr.delay;
            float frameDuration = Math.max(50f, totalDelay / frames.size()) / 1000f;

            for (int i = 0; i < frames.size(); i++) {
                GifFrame f = frames.get(i);

                // 1. Применяем disposal от предыдущего кадра, если он был
                if (i > 0) {
                    GifFrame prevFrame = frames.get(i - 1);
                    if (prevFrame.disposal == 2) {
                        // RESTORE TO BACKGROUND: очищаем прямоугольник предыдущего кадра
                        canvas.setBlending(Pixmap.Blending.None);
                        canvas.setColor(0, 0, 0, 0);
                        canvas.fillRectangle(prevFrame.ix, prevFrame.iy, prevFrame.iw, prevFrame.ih);
                    } else if (prevFrame.disposal == 3 && prevCanvas != null) {
                        // RESTORE TO PREVIOUS: восстанавливаем холст до состояния перед предыдущим кадром
                        canvas.setBlending(Pixmap.Blending.None);
                        canvas.drawPixmap(prevCanvas, 0, 0);
                    }
                }

                // 2. Если у текущего кадра disposal == 3, сохраняем снимок холста ДО наложения кадра
                if (f.disposal == 3) {
                    if (prevCanvas == null) {
                        prevCanvas = new Pixmap(width, height, Pixmap.Format.RGBA8888);
                    }
                    prevCanvas.setBlending(Pixmap.Blending.None);
                    prevCanvas.drawPixmap(canvas, 0, 0);
                }

                // 3. Накладываем текущий кадр поверх холста.
                // SourceOver: прозрачные пиксели (alpha=0) не трогают холст,
                // непрозрачные (alpha=255) перезаписывают его. Для GIF это точное поведение GIF89a.
                canvas.setBlending(Pixmap.Blending.SourceOver);
                canvas.drawPixmap(f.pixmap, f.ix, f.iy);

                // 4. Создаем текстуру из композитного снимка
                Texture texture = new Texture(canvas);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                result.textures.add(texture);
                regions.add(new TextureRegion(texture));

                f.pixmap.dispose();
            }

            canvas.dispose();
            if (prevCanvas != null)
                prevCanvas.dispose();

            result.animation = new Animation<>(frameDuration, regions);
            result.animation.setPlayMode(playMode != null ? playMode : Animation.PlayMode.LOOP);
            return result;
        }
    }
}
