package com.heytozzz.htzcut.neoforge.client.subtitle;

import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.core.subtitle.SubtitleTextEffect;
import com.heytozzz.htzcut.neoforge.HTZCutMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Renders the dialogue subtitle box as a 9-slice panel (corners fixed,
 * edges/center tiled) built from nine 16x16 textures, with the subtitle
 * text animated inside it.
 *
 * Rendering follows the three phases tracked by ClientSubtitleHandler:
 *   1. the box entrance animation (fade/bounce/slide/instant)
 *   2. the text entrance animation OR a typewriter reveal, over the
 *      text phase's duration
 *   3. everything held fully visible until the subtitle expires
 *
 * The box's size is always computed from the FULL subtitle text (never
 * from a partially-revealed typewriter string), so the panel doesn't
 * resize while text is being typed out - only the text drawn inside it
 * grows.
 *
 * Note: uses GuiGraphics#blit's classic (u, v, width, height,
 * textureWidth, textureHeight) overload, and RenderSystem#setShaderColor
 * for the box's fade-in alpha. If a future Minecraft/NeoForge bump
 * changes either of those APIs, only the low-level draw calls here need
 * updating - the phase/progress math is unaffected.
 */
public final class SubtitleHudLayer {

    private static final int TILE = 16;
    private static final int PADDING = 12;
    private static final int MAX_TEXT_WIDTH = 240;
    private static final int MIN_COLS = 4;
    private static final int MIN_ROWS = 3;
    private static final int BOTTOM_MARGIN = 40; // clears the hotbar/xp bar
    private static final int TOP_MARGIN = 10;
    private static final int SLIDE_DISTANCE = 60;
    private static final float BOUNCE_OVERSHOOT = 10f;

    private static final ResourceLocation TOP_LEFT = texture("top_left");
    private static final ResourceLocation TOP = texture("top");
    private static final ResourceLocation TOP_RIGHT = texture("top_right");
    private static final ResourceLocation LEFT = texture("left");
    private static final ResourceLocation CENTER = texture("center");
    private static final ResourceLocation RIGHT = texture("right");
    private static final ResourceLocation BOTTOM_LEFT = texture("bottom_left");
    private static final ResourceLocation BOTTOM = texture("bottom");
    private static final ResourceLocation BOTTOM_RIGHT = texture("bottom_right");

    private SubtitleHudLayer() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(HTZCutMod.MOD_ID, "textures/gui/subtitle_box/" + name + ".png");
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientSubtitleHandler.isActive()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        String fullText = ClientSubtitleHandler.text();
        List<FormattedCharSequence> fullLines = font.split(Component.literal(fullText), MAX_TEXT_WIDTH);
        if (fullLines.isEmpty()) {
            return;
        }

        int textWidth = 0;
        for (FormattedCharSequence line : fullLines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int fullTextHeight = fullLines.size() * font.lineHeight;

        int cols = Math.max(MIN_COLS, ceilDiv(textWidth + PADDING * 2, TILE));
        int rows = Math.max(MIN_ROWS, ceilDiv(fullTextHeight + PADDING * 2, TILE));
        int boxWidth = cols * TILE;
        int boxHeight = rows * TILE;

        int x = (guiGraphics.guiWidth() - boxWidth) / 2;
        int y = ClientSubtitleHandler.position() == SubtitlePosition.TOP
                ? TOP_MARGIN
                : guiGraphics.guiHeight() - boxHeight - BOTTOM_MARGIN;

        // --- Phase 1: box entrance ---
        Transform boxT = computeTransform(
                ClientSubtitleHandler.boxEffect().name(), ClientSubtitleHandler.boxProgress(), boxWidth, boxHeight);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(boxT.dx, boxT.dy, 0);
        RenderSystem.setShaderColor(1f, 1f, 1f, boxT.alpha);
        drawBox(guiGraphics, x, y, cols, rows);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.pose().popPose();

        if (!ClientSubtitleHandler.isTextPhaseStarted()) {
            return; // box is still animating in - text hasn't started yet
        }

        // --- Phase 2/3: text reveal, then hold (hold just keeps textProgress at 1) ---
        SubtitleTextEffect textEffect = ClientSubtitleHandler.textEffect();
        float textProgress = ClientSubtitleHandler.textProgress();

        List<FormattedCharSequence> linesToDraw;
        Transform textT;
        if (textEffect == SubtitleTextEffect.TYPEWRITER_LETTERS || textEffect == SubtitleTextEffect.TYPEWRITER_WORDS) {
            String revealed = reveal(fullText, textProgress, textEffect);
            linesToDraw = font.split(Component.literal(revealed), MAX_TEXT_WIDTH);
            textT = Transform.IDENTITY;
        } else {
            linesToDraw = fullLines;
            textT = computeTransform(textEffect.name(), textProgress, boxWidth, boxHeight);
        }

        int color = 0xFFFFFF | (Math.round(textT.alpha * 255f) << 24);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textT.dx, textT.dy, 0);
        int textY = y + (boxHeight - fullTextHeight) / 2;
        for (FormattedCharSequence line : linesToDraw) {
            int lineWidth = font.width(line);
            int textX = x + (boxWidth - lineWidth) / 2;
            guiGraphics.drawString(font, line, textX, textY, color);
            textY += font.lineHeight;
        }
        guiGraphics.pose().popPose();
    }

    /**
     * Reveals a prefix of {@code fullText} according to progress: either
     * a growing number of characters, or a growing number of whole
     * words (joined back with single spaces).
     */
    private static String reveal(String fullText, float progress, SubtitleTextEffect mode) {
        if (progress >= 1f) {
            return fullText;
        }
        if (mode == SubtitleTextEffect.TYPEWRITER_LETTERS) {
            int count = Math.round(fullText.length() * progress);
            return fullText.substring(0, Math.max(0, Math.min(fullText.length(), count)));
        }

        String[] words = fullText.split(" ");
        int count = Math.round(words.length * progress);
        count = Math.max(0, Math.min(words.length, count));
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, count));
    }

    /**
     * A small (dx, dy, alpha) describing how far an element still is
     * from its final, resting position/opacity at a given effect and
     * progress (0 = not started, 1 = fully in place).
     */
    private record Transform(float dx, float dy, float alpha) {
        static final Transform IDENTITY = new Transform(0f, 0f, 1f);
    }

    private static Transform computeTransform(String effectName, float progress, int width, int height) {
        return switch (effectName) {
            case "FADE_IN" -> new Transform(0f, 0f, progress);
            case "BOUNCE" -> new Transform(0f, (1f - easeOutBack(progress)) * BOUNCE_OVERSHOOT, 1f);
            case "SLIDE_TOP" -> new Transform(0f, -(1f - progress) * (height + SLIDE_DISTANCE), 1f);
            case "SLIDE_BOTTOM" -> new Transform(0f, (1f - progress) * (height + SLIDE_DISTANCE), 1f);
            case "SLIDE_LEFT" -> new Transform(-(1f - progress) * (width + SLIDE_DISTANCE), 0f, 1f);
            case "SLIDE_RIGHT" -> new Transform((1f - progress) * (width + SLIDE_DISTANCE), 0f, 1f);
            default -> Transform.IDENTITY; // INSTANT, and any unrecognized value
        };
    }

    /** Standard "ease out back" - overshoots past 1 before settling, for a light bounce/pop feel. */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float shifted = t - 1f;
        return 1f + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
    }

    private static void drawBox(GuiGraphics g, int x, int y, int cols, int rows) {
        int right = x + (cols - 1) * TILE;
        int bottom = y + (rows - 1) * TILE;

        blit(g, TOP_LEFT, x, y);
        blit(g, TOP_RIGHT, right, y);
        blit(g, BOTTOM_LEFT, x, bottom);
        blit(g, BOTTOM_RIGHT, right, bottom);

        for (int col = 1; col < cols - 1; col++) {
            int tileX = x + col * TILE;
            blit(g, TOP, tileX, y);
            blit(g, BOTTOM, tileX, bottom);
        }

        for (int row = 1; row < rows - 1; row++) {
            int tileY = y + row * TILE;
            blit(g, LEFT, x, tileY);
            blit(g, RIGHT, right, tileY);
        }

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols - 1; col++) {
                blit(g, CENTER, x + col * TILE, y + row * TILE);
            }
        }
    }

    private static void blit(GuiGraphics g, ResourceLocation texture, int x, int y) {
        g.blit(texture, x, y, 0, 0, TILE, TILE, TILE, TILE);
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
