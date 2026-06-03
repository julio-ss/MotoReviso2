package br.jss.motoreviso.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * View customizada que desenha um piloto de moto via Canvas.
 * Suporta cores primária/secundária e visibilidade de equipamentos.
 */
public class AvatarView extends View {

    // Cores
    private int primaryColor   = 0xFF00A8FF;
    private int secondaryColor = 0xFFFFD700;
    private static final int DARK       = 0xFF1C1F23;
    private static final int VERY_DARK  = 0xFF0B0C0E;
    private static final int VISOR_COL  = 0xFF0D1117;
    private static final int BOOT_COL   = 0xFF1A1A2E;
    private static final int GLOVE_COL  = 0xFF1C1F23;

    // Flags de equipamento
    private boolean showHelmet         = true;
    private boolean showBalaclava      = false;
    private boolean showGloves         = true;
    private boolean showBoots          = true;
    private boolean showSpineProtector = false;
    private boolean showAirbagVest     = false;
    private boolean showGlasses        = false;
    private boolean showJaqueta        = true;
    private boolean showCalca          = true;

    // Estilos: 0=integral, 1=modular, 2=offroad
    private int helmetStyle = 0;

    // Número do piloto exibido no peito
    private String riderNumber = "";

    private final Paint paint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  path      = new Path();
    private final RectF rect      = new RectF();

    public AvatarView(Context context) { super(context); init(); }
    public AvatarView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public AvatarView(Context context, AttributeSet attrs, int def) { super(context, attrs, def); init(); }

    private void init() {
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float scale = Math.min(w / 200f, h / 490f);

        canvas.save();
        canvas.translate((w - 200f * scale) / 2f, (h - 490f * scale) / 2f);
        canvas.scale(scale, scale);

        drawPlatform(canvas);
        drawBoots(canvas);
        if (showCalca) drawLegs(canvas);
        if (showSpineProtector) drawSpineHighlight(canvas);
        if (showJaqueta) drawTorso(canvas);
        drawArms(canvas);
        if (showGloves) drawGloves(canvas);
        if (showBalaclava) drawBalaclava(canvas);
        if (showHelmet) drawHelmet(canvas);
        if (showGlasses && helmetStyle == 2) drawGlasses(canvas);
        drawRiderNumber(canvas);

        canvas.restore();
    }

    // ─── Plataforma ─────────────────────────────────────────────
    private void drawPlatform(Canvas canvas) {
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        RadialGradient grad = new RadialGradient(100, 468, 65,
                new int[]{Color.argb(90, r, g, b), Color.argb(30, r, g, b), Color.TRANSPARENT},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(grad);
        paint.setStyle(Paint.Style.FILL);
        rect.set(35, 458, 165, 480);
        canvas.drawOval(rect, paint);
        paint.setShader(null);
    }

    // ─── Botas ──────────────────────────────────────────────────
    private void drawBoots(Canvas canvas) {
        // Sombra
        paint.setColor(Color.argb(60, 0, 0, 0));
        rect.set(57, 462, 102, 480); canvas.drawRoundRect(rect, 6, 6, paint);
        rect.set(98, 462, 143, 480); canvas.drawRoundRect(rect, 6, 6, paint);

        // Bota esquerda
        paint.setColor(BOOT_COL);
        path.reset();
        path.moveTo(65, 422); path.cubicTo(60, 430, 55, 445, 54, 460);
        path.lineTo(54, 472); path.cubicTo(54, 477, 57, 480, 62, 480);
        path.lineTo(100, 480); path.cubicTo(103, 478, 104, 474, 102, 468);
        path.lineTo(100, 440); path.cubicTo(98, 430, 94, 422, 88, 422);
        path.close();
        canvas.drawPath(path, paint);

        // Friso secondary
        paint.setColor(secondaryColor); paint.setAlpha(190);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3f);
        canvas.drawLine(56, 447, 100, 447, paint);
        canvas.drawLine(58, 454, 100, 454, paint);
        paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);

        // Brilho
        paint.setColor(Color.argb(55, 255, 255, 255));
        path.reset();
        path.moveTo(59, 430); path.cubicTo(57, 440, 57, 452, 59, 462);
        path.lineTo(63, 462); path.cubicTo(61, 452, 62, 440, 64, 430);
        path.close();
        canvas.drawPath(path, paint);

        // Bota direita
        paint.setColor(BOOT_COL);
        path.reset();
        path.moveTo(112, 422); path.cubicTo(106, 422, 102, 430, 100, 440);
        path.lineTo(98, 468); path.cubicTo(97, 474, 99, 480, 102, 480);
        path.lineTo(140, 480); path.cubicTo(143, 478, 146, 476, 146, 472);
        path.lineTo(146, 460); path.cubicTo(145, 445, 140, 430, 135, 422);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(secondaryColor); paint.setAlpha(190);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3f);
        canvas.drawLine(100, 447, 144, 447, paint);
        canvas.drawLine(100, 454, 142, 454, paint);
        paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);

        paint.setColor(Color.argb(55, 255, 255, 255));
        path.reset();
        path.moveTo(136, 430); path.cubicTo(138, 440, 139, 452, 137, 462);
        path.lineTo(141, 462); path.cubicTo(143, 452, 142, 440, 140, 430);
        path.close();
        canvas.drawPath(path, paint);
    }

    // ─── Pernas ─────────────────────────────────────────────────
    private void drawLegs(Canvas canvas) {
        // Perna esquerda
        paint.setColor(primaryColor); paint.setStyle(Paint.Style.FILL);
        path.reset();
        path.moveTo(70, 262); path.cubicTo(68, 290, 66, 320, 67, 350);
        path.cubicTo(67, 378, 68, 402, 65, 422);
        path.lineTo(88, 422);
        path.cubicTo(91, 402, 92, 378, 91, 350);
        path.cubicTo(91, 320, 90, 290, 90, 262);
        path.close();
        canvas.drawPath(path, paint);

        // Friso perna esquerda
        paint.setColor(secondaryColor); paint.setAlpha(200);
        path.reset();
        path.moveTo(72, 275); path.cubicTo(71, 305, 71, 335, 72, 365);
        path.lineTo(76, 365); path.cubicTo(75, 335, 76, 305, 76, 275);
        path.close();
        canvas.drawPath(path, paint); paint.setAlpha(255);

        // Joelheira esquerda
        paint.setColor(DARK);
        rect.set(63, 338, 93, 372); canvas.drawRoundRect(rect, 9, 9, paint);
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(Color.argb(130, r, g, b));
        rect.set(67, 342, 89, 368); canvas.drawRoundRect(rect, 7, 7, paint);
        paint.setColor(Color.argb(40, 255, 255, 255));
        rect.set(69, 344, 82, 353); canvas.drawRoundRect(rect, 4, 4, paint);

        // Perna direita
        paint.setColor(primaryColor);
        path.reset();
        path.moveTo(110, 262); path.cubicTo(110, 290, 109, 320, 109, 350);
        path.cubicTo(108, 378, 109, 402, 112, 422);
        path.lineTo(135, 422);
        path.cubicTo(132, 402, 133, 378, 133, 350);
        path.cubicTo(134, 320, 132, 290, 130, 262);
        path.close();
        canvas.drawPath(path, paint);

        // Friso perna direita
        paint.setColor(secondaryColor); paint.setAlpha(200);
        path.reset();
        path.moveTo(124, 275); path.cubicTo(125, 305, 125, 335, 124, 365);
        path.lineTo(128, 365); path.cubicTo(130, 335, 130, 305, 128, 275);
        path.close();
        canvas.drawPath(path, paint); paint.setAlpha(255);

        // Joelheira direita
        paint.setColor(DARK);
        rect.set(107, 338, 137, 372); canvas.drawRoundRect(rect, 9, 9, paint);
        paint.setColor(Color.argb(130, r, g, b));
        rect.set(111, 342, 133, 368); canvas.drawRoundRect(rect, 7, 7, paint);
        paint.setColor(Color.argb(40, 255, 255, 255));
        rect.set(113, 344, 126, 353); canvas.drawRoundRect(rect, 4, 4, paint);
    }

    // ─── Indicador de protetor de coluna ────────────────────────
    private void drawSpineHighlight(Canvas canvas) {
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(Color.argb(70, r, g, b));
        rect.set(84, 135, 116, 230);
        canvas.drawRoundRect(rect, 14, 14, paint);
        paint.setColor(Color.argb(100, 255, 255, 255));
        for (int i = 0; i < 5; i++) {
            rect.set(87, 140 + i * 17, 113, 153 + i * 17);
            canvas.drawRoundRect(rect, 5, 5, paint);
        }
    }

    // ─── Torso ──────────────────────────────────────────────────
    private void drawTorso(Canvas canvas) {
        // Corpo principal do macacão
        paint.setColor(primaryColor); paint.setStyle(Paint.Style.FILL);
        path.reset();
        path.moveTo(63, 116);
        path.cubicTo(58, 145, 58, 185, 62, 225);
        path.cubicTo(64, 244, 68, 258, 70, 262);
        path.lineTo(130, 262);
        path.cubicTo(132, 258, 136, 244, 138, 225);
        path.cubicTo(142, 185, 142, 145, 137, 116);
        path.cubicTo(124, 110, 114, 108, 100, 108);
        path.cubicTo(86, 108, 76, 110, 63, 116);
        path.close();
        canvas.drawPath(path, paint);

        // Costura central
        paint.setColor(Color.argb(80, 0, 0, 0));
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2f);
        canvas.drawLine(100, 116, 100, 258, paint);
        paint.setStyle(Paint.Style.FILL);

        // Faixas diagonais secondary (lado esquerdo)
        paint.setColor(secondaryColor); paint.setAlpha(200);
        path.reset();
        path.moveTo(63, 116); path.lineTo(83, 116); path.lineTo(68, 162); path.lineTo(63, 162);
        path.close(); canvas.drawPath(path, paint);
        // Faixa lado direito
        path.reset();
        path.moveTo(137, 116); path.lineTo(117, 116); path.lineTo(132, 162); path.lineTo(137, 162);
        path.close(); canvas.drawPath(path, paint);
        paint.setAlpha(255);

        // Proteções nos ombros
        paint.setColor(DARK);
        rect.set(57, 110, 80, 134); canvas.drawOval(rect, paint);
        rect.set(120, 110, 143, 134); canvas.drawOval(rect, paint);
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(Color.argb(110, r, g, b));
        rect.set(61, 114, 76, 130); canvas.drawOval(rect, paint);
        rect.set(124, 114, 139, 130); canvas.drawOval(rect, paint);

        // Colete Airbag
        if (showAirbagVest) {
            paint.setColor(Color.argb(90, r, g, b));
            rect.set(72, 124, 128, 205); canvas.drawRoundRect(rect, 12, 12, paint);
            paint.setColor(Color.argb(60, 255, 255, 255));
            canvas.drawCircle(86, 142, 7, paint);
            canvas.drawCircle(114, 142, 7, paint);
            canvas.drawCircle(86, 168, 7, paint);
            canvas.drawCircle(114, 168, 7, paint);
            paint.setColor(secondaryColor); paint.setAlpha(150);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2f);
            rect.set(72, 124, 128, 205); canvas.drawRoundRect(rect, 12, 12, paint);
            paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);
        }
    }

    // ─── Braços ─────────────────────────────────────────────────
    private void drawArms(Canvas canvas) {
        // Braço esquerdo
        paint.setColor(primaryColor); paint.setStyle(Paint.Style.FILL);
        path.reset();
        path.moveTo(63, 116); path.cubicTo(52, 122, 40, 142, 36, 180);
        path.cubicTo(33, 205, 32, 235, 36, 268);
        path.lineTo(54, 268);
        path.cubicTo(51, 238, 52, 210, 55, 185);
        path.cubicTo(58, 158, 64, 136, 68, 120);
        path.close();
        canvas.drawPath(path, paint);

        // Friso braço esquerdo
        paint.setColor(secondaryColor); paint.setAlpha(180);
        path.reset();
        path.moveTo(38, 162); path.cubicTo(36, 188, 35, 212, 38, 238);
        path.lineTo(42, 238); path.cubicTo(40, 212, 41, 188, 44, 162);
        path.close(); canvas.drawPath(path, paint); paint.setAlpha(255);

        // Cotovelo esquerdo
        paint.setColor(DARK);
        rect.set(31, 218, 57, 250); canvas.drawRoundRect(rect, 10, 10, paint);
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(Color.argb(80, r, g, b));
        rect.set(34, 221, 54, 247); canvas.drawRoundRect(rect, 8, 8, paint);

        // Braço direito
        paint.setColor(primaryColor);
        path.reset();
        path.moveTo(137, 116); path.cubicTo(136, 136, 142, 158, 145, 185);
        path.cubicTo(148, 210, 149, 238, 146, 268);
        path.lineTo(164, 268);
        path.cubicTo(168, 235, 167, 205, 164, 180);
        path.cubicTo(160, 142, 148, 122, 137, 116);
        path.close();
        canvas.drawPath(path, paint);

        // Friso braço direito
        paint.setColor(secondaryColor); paint.setAlpha(180);
        path.reset();
        path.moveTo(156, 162); path.cubicTo(158, 188, 159, 212, 158, 238);
        path.lineTo(162, 238); path.cubicTo(164, 212, 163, 188, 161, 162);
        path.close(); canvas.drawPath(path, paint); paint.setAlpha(255);

        // Cotovelo direito
        paint.setColor(DARK);
        rect.set(143, 218, 169, 250); canvas.drawRoundRect(rect, 10, 10, paint);
        paint.setColor(Color.argb(80, r, g, b));
        rect.set(146, 221, 166, 247); canvas.drawRoundRect(rect, 8, 8, paint);
    }

    // ─── Luvas ──────────────────────────────────────────────────
    private void drawGloves(Canvas canvas) {
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);

        // Luva esquerda
        paint.setColor(GLOVE_COL); paint.setStyle(Paint.Style.FILL);
        rect.set(26, 263, 57, 290); canvas.drawRoundRect(rect, 12, 14, paint);
        // Nós
        paint.setColor(Color.argb(120, r, g, b));
        for (int i = 0; i < 4; i++) canvas.drawCircle(30 + i * 7, 270, 3.5f, paint);
        // Detalhe pulso
        paint.setColor(Color.argb(80, 255, 255, 255));
        rect.set(28, 282, 55, 288); canvas.drawRoundRect(rect, 3, 3, paint);

        // Luva direita
        paint.setColor(GLOVE_COL);
        rect.set(143, 263, 174, 290); canvas.drawRoundRect(rect, 12, 14, paint);
        paint.setColor(Color.argb(120, r, g, b));
        for (int i = 0; i < 4; i++) canvas.drawCircle(147 + i * 7, 270, 3.5f, paint);
        paint.setColor(Color.argb(80, 255, 255, 255));
        rect.set(145, 282, 172, 288); canvas.drawRoundRect(rect, 3, 3, paint);
    }

    // ─── Balaclava ──────────────────────────────────────────────
    private void drawBalaclava(Canvas canvas) {
        paint.setColor(DARK); paint.setStyle(Paint.Style.FILL);
        rect.set(82, 92, 118, 116); canvas.drawRoundRect(rect, 12, 12, paint);
        paint.setColor(Color.argb(40, 255, 255, 255));
        rect.set(84, 94, 100, 100); canvas.drawRoundRect(rect, 5, 5, paint);
    }

    // ─── Óculos ─────────────────────────────────────────────────
    private void drawGlasses(Canvas canvas) {
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(Color.argb(190, r, g, b));
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4f);
        rect.set(66, 36, 98, 68); canvas.drawRoundRect(rect, 10, 10, paint);
        rect.set(102, 36, 134, 68); canvas.drawRoundRect(rect, 10, 10, paint);
        canvas.drawLine(98, 52, 102, 52, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(100, 0, 0, 0));
        rect.set(68, 38, 96, 66); canvas.drawRoundRect(rect, 8, 8, paint);
        rect.set(104, 38, 132, 66); canvas.drawRoundRect(rect, 8, 8, paint);
    }

    // ─── Capacetes ──────────────────────────────────────────────
    private void drawHelmet(Canvas canvas) {
        switch (helmetStyle) {
            case 2: drawHelmetOffroad(canvas); break;
            case 1: drawHelmetModular(canvas); break;
            default: drawHelmetIntegral(canvas);
        }
    }

    private void drawHelmetIntegral(Canvas canvas) {
        // Casco principal
        paint.setColor(primaryColor); paint.setStyle(Paint.Style.FILL);
        rect.set(56, 10, 144, 102); canvas.drawOval(rect, paint);

        // Queixo
        path.reset();
        path.moveTo(68, 72); path.cubicTo(65, 84, 66, 96, 74, 102);
        path.lineTo(126, 102); path.cubicTo(134, 96, 135, 84, 132, 72);
        path.close();
        canvas.drawPath(path, paint);

        // Viseira
        paint.setColor(VISOR_COL);
        path.reset();
        path.moveTo(68, 44); path.cubicTo(66, 36, 66, 28, 72, 24);
        path.cubicTo(82, 18, 118, 18, 128, 24);
        path.cubicTo(134, 28, 134, 36, 132, 44);
        path.cubicTo(128, 56, 116, 64, 100, 66);
        path.cubicTo(84, 64, 72, 56, 68, 44);
        path.close();
        canvas.drawPath(path, paint);

        // Reflexo viseira
        paint.setColor(Color.argb(45, 255, 255, 255));
        path.reset();
        path.moveTo(72, 26); path.cubicTo(80, 22, 96, 20, 106, 21);
        path.cubicTo(103, 26, 88, 29, 78, 34);
        path.close(); canvas.drawPath(path, paint);

        // Borda da viseira (secondary)
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(secondaryColor); paint.setAlpha(210);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3.5f);
        path.reset();
        path.moveTo(68, 44); path.cubicTo(66, 36, 66, 28, 72, 24);
        path.cubicTo(82, 18, 118, 18, 128, 24);
        path.cubicTo(134, 28, 134, 36, 132, 44);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);

        // Faixas laterais secondary
        paint.setColor(secondaryColor); paint.setAlpha(200);
        path.reset();
        path.moveTo(56, 44); path.cubicTo(57, 32, 62, 22, 68, 14);
        path.lineTo(72, 16); path.cubicTo(66, 26, 62, 36, 62, 46); path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(144, 44); path.cubicTo(143, 32, 138, 22, 132, 14);
        path.lineTo(128, 16); path.cubicTo(134, 26, 138, 36, 138, 46); path.close();
        canvas.drawPath(path, paint);
        paint.setAlpha(255);

        // Saídas de ar no topo
        paint.setColor(DARK);
        rect.set(86, 12, 114, 22); canvas.drawRoundRect(rect, 4, 4, paint);
        paint.setColor(Color.argb(90, r, g, b));
        rect.set(88, 13, 112, 21); canvas.drawRoundRect(rect, 3, 3, paint);
    }

    private void drawHelmetModular(Canvas canvas) {
        drawHelmetIntegral(canvas);
        // Linha de articulação do flip-up
        paint.setColor(DARK); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2f);
        canvas.drawLine(66, 70, 134, 70, paint);
        // Dobradiças
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(66, 70, 5, paint);
        canvas.drawCircle(134, 70, 5, paint);
        paint.setColor(secondaryColor); paint.setAlpha(160);
        canvas.drawCircle(66, 70, 3, paint);
        canvas.drawCircle(134, 70, 3, paint);
        paint.setAlpha(255);
    }

    private void drawHelmetOffroad(Canvas canvas) {
        paint.setColor(primaryColor); paint.setStyle(Paint.Style.FILL);
        rect.set(58, 18, 142, 98); canvas.drawOval(rect, paint);

        // Pala/viseira sun shield
        paint.setColor(darken(primaryColor, 0.25f));
        path.reset();
        path.moveTo(56, 32); path.cubicTo(52, 26, 47, 20, 46, 18);
        path.lineTo(154, 18); path.cubicTo(153, 20, 148, 26, 144, 32); path.close();
        canvas.drawPath(path, paint);

        // Faixa na pala
        paint.setColor(secondaryColor); paint.setAlpha(200);
        rect.set(52, 18, 148, 25); canvas.drawRect(rect, paint);
        paint.setAlpha(255);

        // Goggle maior
        paint.setColor(VISOR_COL);
        rect.set(65, 40, 135, 74); canvas.drawRoundRect(rect, 18, 18, paint);

        // Aro do goggle
        int r = Color.red(secondaryColor), g = Color.green(secondaryColor), b = Color.blue(secondaryColor);
        paint.setColor(Color.argb(200, r, g, b));
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4.5f);
        rect.set(65, 40, 135, 74); canvas.drawRoundRect(rect, 18, 18, paint);
        paint.setStyle(Paint.Style.FILL);

        // Reflexo goggle
        paint.setColor(Color.argb(50, 255, 255, 255));
        path.reset();
        path.moveTo(72, 44); path.cubicTo(82, 40, 100, 40, 112, 44);
        path.cubicTo(108, 48, 88, 48, 72, 44); path.close();
        canvas.drawPath(path, paint);

        // Queixo e barra
        paint.setColor(DARK);
        rect.set(72, 84, 128, 102); canvas.drawRoundRect(rect, 10, 10, paint);
        paint.setColor(Color.argb(50, 255, 255, 255));
        rect.set(76, 87, 124, 92); canvas.drawRoundRect(rect, 5, 5, paint);
    }

    // ─── Número no peito ────────────────────────────────────────
    private void drawRiderNumber(Canvas canvas) {
        if (riderNumber == null || riderNumber.isEmpty()) return;
        // Badge background
        paint.setColor(secondaryColor); paint.setStyle(Paint.Style.FILL);
        rect.set(78, 188, 122, 222); canvas.drawRoundRect(rect, 9, 9, paint);
        // Sombra no badge
        paint.setColor(Color.argb(60, 0, 0, 0));
        rect.set(80, 192, 120, 222); canvas.drawRoundRect(rect, 7, 7, paint);
        // Texto
        textPaint.setColor(VERY_DARK);
        textPaint.setTextSize(riderNumber.length() <= 2 ? 28f : 20f);
        canvas.drawText(riderNumber, 100, 214, textPaint);
    }

    // ─── Helper ─────────────────────────────────────────────────
    private int darken(int color, float factor) {
        return Color.argb(Color.alpha(color),
                Math.max(0, (int)(Color.red(color)   * (1 - factor))),
                Math.max(0, (int)(Color.green(color) * (1 - factor))),
                Math.max(0, (int)(Color.blue(color)  * (1 - factor))));
    }

    // ─── Setters públicos ────────────────────────────────────────
    public void setPrimaryColor(int c)   { primaryColor = c;   invalidate(); }
    public void setSecondaryColor(int c) { secondaryColor = c; invalidate(); }
    public void setHelmetStyle(int s)    { helmetStyle = s;    invalidate(); }
    public void setRiderNumber(String n) { riderNumber = n != null ? n : ""; invalidate(); }
    public void setShowHelmet(boolean v)         { showHelmet = v;         invalidate(); }
    public void setShowBalaclava(boolean v)      { showBalaclava = v;      invalidate(); }
    public void setShowGloves(boolean v)         { showGloves = v;         invalidate(); }
    public void setShowBoots(boolean v)          { showBoots = v;          invalidate(); }
    public void setShowSpineProtector(boolean v) { showSpineProtector = v; invalidate(); }
    public void setShowAirbagVest(boolean v)     { showAirbagVest = v;     invalidate(); }
    public void setShowGlasses(boolean v)        { showGlasses = v;        invalidate(); }
    public void setShowJaqueta(boolean v)        { showJaqueta = v;        invalidate(); }
    public void setShowCalca(boolean v)          { showCalca = v;          invalidate(); }

    // Getters para salvar estado
    public int  getPrimaryColor()   { return primaryColor; }
    public int  getSecondaryColor() { return secondaryColor; }
    public int  getHelmetStyle()    { return helmetStyle; }
    public boolean isShowBalaclava()      { return showBalaclava; }
    public boolean isShowGloves()         { return showGloves; }
    public boolean isShowBoots()          { return showBoots; }
    public boolean isShowSpineProtector() { return showSpineProtector; }
    public boolean isShowAirbagVest()     { return showAirbagVest; }
    public boolean isShowGlasses()        { return showGlasses; }
}
