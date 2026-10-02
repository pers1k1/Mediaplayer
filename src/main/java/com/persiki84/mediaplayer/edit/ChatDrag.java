package com.persiki84.mediaplayer.edit;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.anim.Smooth;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandPlacement;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.island.IslandBounds;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Paint;
import com.persiki84.mediaplayer.render.Weight;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ChatDrag {
    private static final float SNAP_UNITS = 6.0f;
    private static final float SCALE_STEP = 0.05f;
    private static final float HOVER_SPEED = 12.0f;
    private static final float OUTLINE = 1.0f;
    private static final float HINT_SCALE = 0.7f;
    private static final float HINT_GAP = 4.0f;
    private static final Component HINT = Component.translatable("mediaplayer.drag.hint");

    private static final Smooth hover = new Smooth(0.0f, HOVER_SPEED);
    private static boolean dragging;
    private static float grabX;
    private static float grabY;
    private static boolean snappedX;

    private ChatDrag() {}

    public static void attach(Screen screen) {
        if (!(screen instanceof ChatScreen)) return;

        dragging = false;
        ScreenMouseEvents.allowMouseClick(screen).register(ChatDrag::press);
        ScreenMouseEvents.allowMouseDrag(screen).register((current, event, dx, dy) -> !drag(current, event));
        ScreenMouseEvents.allowMouseRelease(screen).register((current, event) -> !release());
        ScreenMouseEvents.allowMouseScroll(screen).register((current, x, y, across, down) -> !scroll(x, y, down));
        ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) ->
                overlay(graphics, mouseX, mouseY));
    }

    private static boolean press(Screen screen, MouseButtonEvent event) {
        if (!IslandBounds.contains(event.x(), event.y())) return true;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            IslandSettings.place(IslandPlacement.DEFAULT);
            return false;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;

        dragging = true;
        grabX = (float) event.x() - (IslandBounds.left() + IslandBounds.width() / 2.0f);
        grabY = (float) event.y() - IslandBounds.top();
        return false;
    }

    private static boolean drag(Screen screen, MouseButtonEvent event) {
        if (!dragging) return false;

        float center = (float) event.x() - grabX;
        float top = (float) event.y() - grabY;
        snappedX = Math.abs(center - screen.width / 2.0f) <= SNAP_UNITS;
        if (snappedX) center = screen.width / 2.0f;
        if (top <= SNAP_UNITS) top = 0.0f;
        IslandSettings.place(IslandSettings.placement().moved(center / screen.width, top / screen.height));
        return true;
    }

    private static boolean release() {
        boolean was = dragging;
        dragging = false;
        snappedX = false;
        return was;
    }

    private static boolean scroll(double x, double y, double amount) {
        if (!IslandBounds.contains(x, y) || amount == 0.0) return false;

        IslandPlacement placement = IslandSettings.placement();
        float next = Math.round((placement.scale() + Math.signum(amount) * SCALE_STEP) * 100.0f) / 100.0f;
        IslandSettings.place(placement.scaled(next));
        return true;
    }

    private static void overlay(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean near = dragging || IslandBounds.contains(mouseX, mouseY);
        float shown = hover.to(near ? 1.0f : 0.0f, FrameClock.delta());
        if (shown <= 0.01f || !IslandBounds.shown()) return;

        outline(graphics, shown);
        if (dragging && snappedX) guide(graphics, shown);
        float hintWidth = Ink.width(Minecraft.getInstance().font, HINT, Weight.REGULAR, HINT_SCALE);
        float hintX = Anim.clamp(IslandBounds.left() + (IslandBounds.width() - hintWidth) / 2.0f, 2.0f,
                graphics.guiWidth() - hintWidth - 2.0f);
        Ink.label(graphics, Minecraft.getInstance().font, HINT, Weight.REGULAR, hintX,
                IslandBounds.top() + IslandBounds.height() + HINT_GAP, HINT_SCALE,
                Colors.alpha(Palette.INK_DIM, shown), 0.0f);
    }

    private static void outline(GuiGraphics graphics, float shown) {
        int edge = Colors.withAlpha(Palette.WHITE, 0.35f * shown);
        float left = IslandBounds.left() - 2.0f;
        float top = IslandBounds.top() - 2.0f;
        float width = IslandBounds.width() + 4.0f;
        float height = IslandBounds.height() + 4.0f;
        Paint.shape(graphics, left, top, width, OUTLINE, OUTLINE / 2.0f, edge, edge, 0.0f);
        Paint.shape(graphics, left, top + height - OUTLINE, width, OUTLINE, OUTLINE / 2.0f, edge, edge, 0.0f);
        Paint.shape(graphics, left, top, OUTLINE, height, OUTLINE / 2.0f, edge, edge, 0.0f);
        Paint.shape(graphics, left + width - OUTLINE, top, OUTLINE, height, OUTLINE / 2.0f, edge, edge, 0.0f);
    }

    private static void guide(GuiGraphics graphics, float shown) {
        int line = Colors.withAlpha(Palette.ACCENT, 0.5f * shown);
        float center = graphics.guiWidth() / 2.0f;
        Paint.shape(graphics, center - OUTLINE / 2.0f, 0.0f, OUTLINE, graphics.guiHeight(), 0.0f, line, line, 0.0f);
    }
}
