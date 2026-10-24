package com.wallet.desktop.ui.component;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class Icons {

    public static final Paint LIME = Color.web("#C8F169");
    public static final Paint CORAL = Color.web("#F2A88A");
    public static final Paint IVORY = Color.web("#F5F1E8");
    public static final Paint MUTED = Color.web("#AAB5C6");
    public static final Paint NAVY = Color.web("#101A2B");

    public static SVGPath create(String content, Paint stroke, double strokeWidth) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(stroke);
        path.setStrokeWidth(strokeWidth);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    public static SVGPath createFilled(String content, Paint fill) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(fill);
        path.setStroke(Color.TRANSPARENT);
        return path;
    }

    public static Node arrowUpRight(Paint color, double size) {
        SVGPath path = create("M7 17L17 7M17 7H7M17 7V17", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    public static Node arrowDownLeft(Paint color, double size) {
        SVGPath path = create("M17 7L7 17M7 17H17M7 17V7", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    public static Node arrowRight(Paint color, double size) {
        SVGPath path = create("M5 12h14M12 5l7 7-7 7", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node check(Paint color, double size) {
        SVGPath path = create("M20 6L9 17l-5-5", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    public static Node checkCircle(Paint color, double size) {
        SVGPath path = create("M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M9 12l2 2 4-4", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node alertCircle(Paint color, double size) {
        SVGPath path = create("M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M12 8v4 M12 16h.01", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node shieldCheck(Paint color, double size) {
        SVGPath path = create("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node wallet(Paint color, double size) {
        SVGPath path = create("M21 12V7H5a2 2 0 0 1 0-4h14v4 M3 5v14a2 2 0 0 0 2 2h16v-5 M18 12a2 2 0 0 0 0 4h4v-4z", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node send(Paint color, double size) {
        SVGPath path = create("m22 2-7 20-4-9-9-4Z M22 2 11 13", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node history(Paint color, double size) {
        SVGPath path = create("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5 M12 7v5l4 2", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node user(Paint color, double size) {
        SVGPath path = create("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node logOut(Paint color, double size) {
        SVGPath path = create("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node coins(Paint color, double size) {
        SVGPath path = create("M12 2C6.48 2 2 4.24 2 7s4.48 5 10 5 10-2.24 10-5-4.48-5-10-5zm0 14c-5.52 0-10-2.24-10-5v3c0 2.76 4.48 5 10 5s10-2.24 10-5v-3c0 2.76-4.48 5-10 5zm0 6c-5.52 0-10-2.24-10-5v3c0 2.76 4.48 5 10 5s10-2.24 10-5v-3c0 2.76-4.48 5-10 5z", color, 1.8);
        scaleTo(path, size);
        return path;
    }

    public static Node copy(Paint color, double size) {
        SVGPath path = create("M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2 M16 4h-2a2 2 0 0 1-2-2 2 2 0 0 1 2-2h2a2 2 0 0 1 2 2 2 2 0 0 1-2 2z", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node refresh(Paint color, double size) {
        SVGPath path = create("M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16 M3 21v-5h5 M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8 M21 3v5h-5", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node zap(Paint color, double size) {
        SVGPath path = create("M13 2 3 14h9l-1 8 10-12h-9l1-8z", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node lock(Paint color, double size) {
        SVGPath path = create("M7 11V7a5 5 0 0 1 10 0v4 M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node mail(Paint color, double size) {
        SVGPath path = create("m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7 M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node keyRound(Paint color, double size) {
        SVGPath path = create("M2 18v3c0 .6.4 1 1 1h4v-3h3v-3h2l1.4-1.4a6.5 6.5 0 1 0-4-4Z M16.5 7.5v.01", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node externalLink(Paint color, double size) {
        SVGPath path = create("M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6 M15 3h6v6 M10 14 21 3", color, 2.0);
        scaleTo(path, size);
        return path;
    }

    public static Node chevronLeft(Paint color, double size) {
        SVGPath path = create("m15 18-6-6 6-6", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    public static Node chevronRight(Paint color, double size) {
        SVGPath path = create("m9 18 6-6-6-6", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    public static Node plus(Paint color, double size) {
        SVGPath path = create("M5 12h14 M12 5v14", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    public static Node x(Paint color, double size) {
        SVGPath path = create("M18 6 6 18 M6 6l12 12", color, 2.5);
        scaleTo(path, size);
        return path;
    }

    private static void scaleTo(SVGPath path, double size) {
        double factor = size / 24.0;
        path.setScaleX(factor);
        path.setScaleY(factor);
    }
}
