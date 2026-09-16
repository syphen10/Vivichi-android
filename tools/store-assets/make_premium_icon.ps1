param([string]$OutPath)

# Play one-time product icon: 512x512 32-bit PNG, no text/branding (Play requirement).
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @("System.Drawing") -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public static class PremiumIcon {
    static PointF P(float x, float y) { return new PointF(x, y); }

    static void Poly(Graphics g, Color c, params PointF[] pts) {
        using (var b = new SolidBrush(c)) g.FillPolygon(b, pts);
    }

    static void Sparkle(Graphics g, float cx, float cy, float r, int alpha) {
        float i = r * 0.26f;
        var pts = new PointF[8];
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 - Math.PI / 2;
            float rr = (k % 2 == 0) ? r : i;
            pts[k] = P(cx + (float)Math.Cos(a) * rr, cy + (float)Math.Sin(a) * rr);
        }
        using (var b = new SolidBrush(Color.FromArgb(alpha, 255, 255, 255))) g.FillPolygon(b, pts);
    }

    public static void Render(string outPath) {
        const int S = 512;
        using (var bmp = new Bitmap(S, S, PixelFormat.Format32bppArgb))
        using (var g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.Clear(Color.Transparent);

            // rounded gradient tile: Vivichi premium gold -> pink -> purple
            var tile = new RectangleF(16, 16, S - 32, S - 32);
            float r = 110, d = r * 2;
            using (var path = new GraphicsPath()) {
                path.AddArc(tile.X, tile.Y, d, d, 180, 90);
                path.AddArc(tile.Right - d, tile.Y, d, d, 270, 90);
                path.AddArc(tile.Right - d, tile.Bottom - d, d, d, 0, 90);
                path.AddArc(tile.X, tile.Bottom - d, d, d, 90, 90);
                path.CloseFigure();
                using (var lg = new LinearGradientBrush(tile, Color.FromArgb(255, 200, 87), Color.FromArgb(179, 136, 255), 45f)) {
                    var blend = new ColorBlend(3);
                    blend.Colors = new[] { Color.FromArgb(255, 200, 87), Color.FromArgb(255, 126, 179), Color.FromArgb(179, 136, 255) };
                    blend.Positions = new[] { 0f, 0.5f, 1f };
                    lg.InterpolationColors = blend;
                    g.FillPath(lg, path);
                }
                // soft top highlight
                using (var hl = new PathGradientBrush(new[] { P(16, 16), P(S - 16, 16), P(S - 16, 260), P(16, 260) })) {
                    hl.CenterColor = Color.FromArgb(70, 255, 255, 255);
                    hl.SurroundColors = new[] { Color.FromArgb(0, 255, 255, 255) };
                    var st = g.Save(); g.SetClip(path); g.FillRectangle(hl, 16, 16, S - 32, 244); g.Restore(st);
                }
            }

            // glow behind the gem
            using (var gp = new GraphicsPath()) {
                gp.AddEllipse(96, 110, 320, 300);
                using (var pg = new PathGradientBrush(gp)) {
                    pg.CenterColor = Color.FromArgb(120, 255, 255, 255);
                    pg.SurroundColors = new[] { Color.FromArgb(0, 255, 255, 255) };
                    g.FillPath(pg, gp);
                }
            }

            // gem geometry (brilliant cut, front view)
            float top = 168, girdle = 238, tip = 392, left = 136, right = 376, cx = 256;
            float tl = 196, tr = 316;              // table edges
            var shadow = Color.FromArgb(60, 60, 20, 90);
            Poly(g, shadow, P(left + 6, girdle + 10), P(tl + 6, top + 10), P(tr + 6, top + 10), P(right + 6, girdle + 10), P(cx + 6, tip + 12));

            // crown facets
            Poly(g, Color.FromArgb(125, 211, 252), P(left, girdle), P(tl, top), P(cx - 30, girdle));
            Poly(g, Color.FromArgb(186, 230, 253), P(tl, top), P(tr, top), P(cx, girdle - 2), P(cx - 30, girdle));
            Poly(g, Color.FromArgb(224, 242, 254), P(tl, top), P(tr, top), P(cx + 30, girdle), P(cx, girdle - 2));
            Poly(g, Color.FromArgb(56, 189, 248), P(tr, top), P(right, girdle), P(cx + 30, girdle));
            // pavilion facets
            Poly(g, Color.FromArgb(14, 165, 233), P(left, girdle), P(cx - 30, girdle), P(cx, tip));
            Poly(g, Color.FromArgb(56, 189, 248), P(cx - 30, girdle), P(cx + 30, girdle), P(cx, tip));
            Poly(g, Color.FromArgb(2, 132, 199), P(cx + 30, girdle), P(right, girdle), P(cx, tip));
            // facet edges
            using (var pen = new Pen(Color.FromArgb(150, 255, 255, 255), 3f) { LineJoin = LineJoin.Round }) {
                g.DrawPolygon(pen, new[] { P(left, girdle), P(tl, top), P(tr, top), P(right, girdle), P(cx, tip) });
                g.DrawLine(pen, left, girdle, right, girdle);
                g.DrawLine(pen, cx - 30, girdle, cx, tip);
                g.DrawLine(pen, cx + 30, girdle, cx, tip);
                g.DrawLine(pen, tl, top, cx - 30, girdle);
                g.DrawLine(pen, tr, top, cx + 30, girdle);
            }
            // shine streak on the table
            Poly(g, Color.FromArgb(200, 255, 255, 255), P(tl + 18, top + 8), P(tl + 46, top + 8), P(tl + 22, girdle - 20));

            Sparkle(g, 384, 142, 34, 235);
            Sparkle(g, 128, 150, 20, 200);
            Sparkle(g, 400, 350, 16, 180);
            Sparkle(g, 118, 344, 12, 160);

            bmp.Save(outPath, ImageFormat.Png);
        }
    }
}
"@

[PremiumIcon]::Render($OutPath)
$OutPath
