package com.fiskmods.heroes.common.util;

import java.util.ArrayList;
import java.util.List;

/** Screen-space layout shared by drawing and hit testing; independent of Minecraft's client. */
public final class SuitHudLayout
{
    public static final int MARGIN = 4;
    public static final int HEADER_HEIGHT = 12;
    public static final int ROW_HEIGHT = 11;
    public static final int ROW_DRAW_HEIGHT = 10;
    private static final int COLUMN_GAP = 4;
    private static final int BOTTOM_RESERVE = 60;

    public record Rect(int x, int y, int width, int height)
    {
        public boolean contains(double px, double py)
        {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    public record Layout(int titleX, int titleY, int titleWidth, List<Rect> rows) {}

    private SuitHudLayout() {}

    /** Row widths are the desired text/background widths, in GUI-scaled pixels. */
    public static Layout calculate(int screenWidth, int screenHeight, List<Integer> desiredWidths)
    {
        return calculate(screenWidth, screenHeight, desiredWidths, BOTTOM_RESERVE);
    }

    /** Additional bottom space accommodates health rows and the suit/vanilla armor meters. */
    public static Layout calculate(int screenWidth, int screenHeight, List<Integer> desiredWidths, int bottomReserve)
    {
        int width = Math.max(1, screenWidth - 2 * MARGIN);
        int reserve = Math.min(Math.max(BOTTOM_RESERVE, bottomReserve),
                Math.max(0, screenHeight - HEADER_HEIGHT - ROW_HEIGHT - 2 * MARGIN));
        int bottom = Math.max(MARGIN + HEADER_HEIGHT + ROW_HEIGHT, screenHeight - reserve);
        int maxRows = Math.max(1, (bottom - MARGIN - HEADER_HEIGHT) / ROW_HEIGHT);
        int count = desiredWidths.size();
        int columns = Math.max(1, (count + maxRows - 1) / maxRows);
        int visibleRows = Math.min(count, maxRows);
        int top = Math.max(MARGIN, bottom - HEADER_HEIGHT - visibleRows * ROW_HEIGHT);
        int usable = Math.max(columns, width - (columns - 1) * COLUMN_GAP);
        int[] columnWidths = new int[columns];
        int desiredTotal = 0;
        for (int col = 0; col < columns; ++col)
        {
            int desired = 1;
            for (int i = col * maxRows; i < Math.min(count, (col + 1) * maxRows); ++i)
                desired = Math.max(desired, desiredWidths.get(i));
            columnWidths[col] = desired;
            desiredTotal += desired;
        }
        if (desiredTotal > usable)
        {
            // Share space evenly when multiple columns cannot fit at their natural width.
            for (int col = 0; col < columns; ++col)
                columnWidths[col] = usable / columns + (col < usable % columns ? 1 : 0);
        }
        int titleWidth = Math.min(width, Math.max(90, desiredTotal + (columns - 1) * COLUMN_GAP));
        List<Rect> rows = new ArrayList<>(count);
        int x = MARGIN;
        for (int col = 0; col < columns; ++col)
        {
            for (int row = 0; row < maxRows && col * maxRows + row < count; ++row)
                rows.add(new Rect(x, top + HEADER_HEIGHT + row * ROW_HEIGHT - 1, columnWidths[col], ROW_DRAW_HEIGHT));
            x += columnWidths[col] + COLUMN_GAP;
        }
        return new Layout(MARGIN, top, titleWidth, List.copyOf(rows));
    }
}
