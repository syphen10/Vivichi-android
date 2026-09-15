param([string]$OutDir, [string]$FontDir, [string]$RawDir)

Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @("System.Drawing") -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;

public static class Shots {
    const int W = 1080, H = 1920;

    static GraphicsPath RoundRect(RectangleF r, float rad) {
        var p = new GraphicsPath(); float d = rad * 2;
        p.AddArc(r.X, r.Y, d, d, 180, 90); p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
        p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90); p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
        p.CloseFigure(); return p;
    }

    static void Glow(Graphics g, float cx, float cy, float rx, float ry, Color c, int alpha) {
        using (var path = new GraphicsPath()) {
            path.AddEllipse(cx - rx, cy - ry, rx * 2, ry * 2);
            using (var br = new PathGradientBrush(path)) {
                br.CenterColor = Color.FromArgb(alpha, c);
                br.SurroundColors = new[] { Color.FromArgb(0, c) };
                br.FocusScales = new PointF(0.1f, 0.1f);
                g.FillPath(br, path);
            }
        }
    }

    // Soft shadow: draw the shape tiny, then upscale with bicubic filtering, which blurs it.
    static void SoftShadow(Graphics g, RectangleF r, float rad, float blur, int alpha) {
        const int k = 10;
        int sw = (int)((r.Width + blur * 4) / k), sh = (int)((r.Height + blur * 4) / k);
        using (var small = new Bitmap(sw, sh, PixelFormat.Format32bppArgb))
        using (var sg = Graphics.FromImage(small)) {
            sg.SmoothingMode = SmoothingMode.AntiAlias;
            var inner = new RectangleF(blur * 2 / k, blur * 2 / k, r.Width / k, r.Height / k);
            using (var p = RoundRect(inner, rad / k))
            using (var b = new SolidBrush(Color.FromArgb(alpha, 70, 30, 70))) sg.FillPath(b, p);
            g.DrawImage(small, r.X - blur * 2, r.Y - blur * 2, sw * k, sh * k);
        }
    }

    static GraphicsPath Headline(string l1, string l2, FontFamily f, float size, float y1, float y2) {
        var fmt = new StringFormat(StringFormat.GenericTypographic) { Alignment = StringAlignment.Center };
        var p = new GraphicsPath();
        // layout boxes must be taller than one line of the font or GDI+ drops the text
        p.AddString(l1, f, 0, size, new RectangleF(-400, y1, W + 800, size * 2), fmt);
        p.AddString(l2, f, 0, size, new RectangleF(-400, y2, W + 800, size * 2), fmt);
        return p;
    }

    public static void Render(string shotPath, string outPath, string line1, string line2, string sub,
                              Color top, Color bottom, Color glowA, Color glowB,
                              FontFamily black, FontFamily xbold, RectangleF callout) {
        using (var bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (var g = Graphics.FromImage(bmp))
        using (var shot = Image.FromFile(shotPath)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.TextRenderingHint = TextRenderingHint.AntiAlias;

            using (var bg = new LinearGradientBrush(new Rectangle(0, 0, W, H), top, bottom, 70f)) g.FillRectangle(bg, 0, 0, W, H);
            Glow(g, 140, 260, 460, 380, glowA, 130);
            Glow(g, 980, 1250, 520, 620, glowB, 120);
            Glow(g, 540, 1900, 700, 260, Color.White, 90);
            using (var dot = new SolidBrush(Color.FromArgb(160, 255, 255, 255)))
                foreach (var p in new[] { new PointF(96, 470), new PointF(990, 150), new PointF(1010, 620), new PointF(70, 1180), new PointF(1000, 1680), new PointF(160, 1760) })
                    g.FillEllipse(dot, p.X - 7, p.Y - 7, 14, 14);

            // headline, auto-shrunk so the wider line fits within 960px
            float size = 104f;
            using (var probe = Headline(line1, line2, black, size, 92, 206)) {
                float wmax = Math.Max(MeasureLine(line1, black, size), MeasureLine(line2, black, size));
                if (wmax > 960f) size = size * 960f / wmax;
            }
            float lineGap = size * 1.1f;
            using (var path = Headline(line1, line2, black, size, 92 + (104f - size) * 0.5f, 92 + (104f - size) * 0.5f + lineGap)) {
                var b = path.GetBounds();
                using (var shadow = (GraphicsPath)path.Clone())
                using (var m = new Matrix()) {
                    m.Translate(0, 6); shadow.Transform(m);
                    using (var sb = new SolidBrush(Color.FromArgb(34, 110, 40, 80))) g.FillPath(sb, shadow);
                }
                using (var tg = new LinearGradientBrush(new RectangleF(b.X, b.Y, b.Width, b.Height), Color.FromArgb(236, 96, 128), Color.FromArgb(110, 40, 86), 30f))
                    g.FillPath(tg, path);
            }
            var cfmt = new StringFormat(StringFormat.GenericTypographic) { Alignment = StringAlignment.Center };
            using (var f = new Font(xbold, 46f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (var sbr = new SolidBrush(Color.FromArgb(61, 46, 78)))
                g.DrawString(sub, f, sbr, new RectangleF(20, 350, W - 40, 110), cfmt);

            // phone, screen matched to the screenshot's own aspect so nothing is cropped
            float screenW = 620f;
            float screenH = screenW * shot.Height / shot.Width;
            float bezel = 22f;
            var frame = new RectangleF((W - screenW) / 2 - bezel, 466, screenW + bezel * 2, screenH + bezel * 2);
            var screen = new RectangleF(frame.X + bezel, frame.Y + bezel, screenW, screenH);

            SoftShadow(g, new RectangleF(frame.X + 10, frame.Y + 40, frame.Width - 20, frame.Height), 84, 60, 150);
            using (var fp = RoundRect(frame, 84))
            using (var fb = new LinearGradientBrush(frame, Color.FromArgb(58, 44, 74), Color.FromArgb(28, 20, 38), 90f)) g.FillPath(fb, fp);
            using (var edge = RoundRect(new RectangleF(frame.X + 3, frame.Y + 3, frame.Width - 6, frame.Height - 6), 81))
            using (var ep = new Pen(Color.FromArgb(70, 255, 255, 255), 2f)) g.DrawPath(ep, edge);

            using (var sp = RoundRect(screen, 62)) {
                var state = g.Save();
                g.SetClip(sp);
                g.DrawImage(shot, screen);
                g.Restore(state);
            }

            // optional magnified callout of part of the screenshot
            if (callout.Width > 0) {
                float cw = 940f;
                float ch = cw * callout.Height / callout.Width;
                var dst = new RectangleF((W - cw) / 2, 1330, cw, ch);
                var border = new RectangleF(dst.X - 14, dst.Y - 14, dst.Width + 28, dst.Height + 28);
                SoftShadow(g, new RectangleF(border.X + 10, border.Y + 26, border.Width - 20, border.Height), 46, 50, 170);
                using (var bp = RoundRect(border, 46))
                using (var bb = new SolidBrush(Color.White)) g.FillPath(bb, bp);
                using (var cp = RoundRect(dst, 34)) {
                    var state = g.Save();
                    g.SetClip(cp);
                    g.DrawImage(shot, dst, callout, GraphicsUnit.Pixel);
                    g.Restore(state);
                }
            }

            bmp.Save(outPath, ImageFormat.Png);
        }
    }

    static float MeasureLine(string s, FontFamily f, float size) {
        using (var p = new GraphicsPath()) {
            p.AddString(s, f, 0, size, new PointF(0, 0), StringFormat.GenericTypographic);
            return p.GetBounds().Width;
        }
    }
}
"@

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$black = New-Object System.Drawing.Text.PrivateFontCollection; $black.AddFontFile((Join-Path $FontDir "Nunito-Black.ttf"))
$xbold = New-Object System.Drawing.Text.PrivateFontCollection; $xbold.AddFontFile((Join-Path $FontDir "Nunito-ExtraBold.ttf"))
function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$none = New-Object System.Drawing.RectangleF(0, 0, 0, 0)
$panel = New-Object System.Drawing.RectangleF(179, 830, 817, 252)

$slides = @(
  @{ f="home.png";        o="01-home.png";     l1="Your buddy lives"; l2="on your habits";   s="Keep them happy, healthy and alive";     t="#FDF4E9"; b="#FFD6E4"; ga="#FFC486"; gb="#FF9EBB"; c=$none },
  @{ f="habits.png";      o="02-habits.png";   l1="Build habits";     l2="that stick";       s="Check them off before they expire";      t="#FFE6F0"; b="#E4D9FF"; ga="#FFB3C8"; gb="#C4B0FF"; c=$none },
  @{ f="buddies.png";     o="03-buddies.png";  l1="Unlock 16";        l2="adorable buddies"; s="From foxes and koalas to a dragon";      t="#EEE6FF"; b="#FFE3EE"; ga="#D4C4FF"; gb="#FFB3C8"; c=$none },
  @{ f="coins.png";       o="04-coins.png";    l1="Earn coins";       l2="every day";        s="Finish habits to fill your coin jar";    t="#FFF4D6"; b="#FFE0EC"; ga="#FFD166"; gb="#FF9EBB"; c=$none },
  @{ f="themes.png";      o="05-themes.png";   l1="Make it";          l2="all yours";        s="Outfits, free seasons & colour themes";  t="#DDF1FB"; b="#EDE4FF"; ga="#A8D8FF"; gb="#C4B0FF"; c=$none },
  @{ f="shade_clean.png"; o="06-panel.png";    l1="Check in";         l2="with one swipe";   s="Health & habit countdown in your shade"; t="#EAE4FF"; b="#FFDDE9"; ga="#C4B0FF"; gb="#FF9EBB"; c=$panel },
  @{ f="play.png";        o="07-play.png";     l1="Play with";        l2="your buddy";       s="Feed, pet, hug, sing and tickle";        t="#FFEBD9"; b="#FFD1DF"; ga="#FFD166"; gb="#FF85A2"; c=$none },
  @{ f="stats.png";       o="08-stats.png";    l1="Track streaks,";   l2="earn titles";      s="Watch your progress grow every day";     t="#DFF7ED"; b="#E6DEFF"; ga="#A8E6CF"; gb="#C4B0FF"; c=$none }
)
foreach ($sl in $slides) {
  [Shots]::Render((Join-Path $RawDir $sl.f), (Join-Path $OutDir $sl.o), $sl.l1, $sl.l2, $sl.s, (C $sl.t), (C $sl.b), (C $sl.ga), (C $sl.gb), $black.Families[0], $xbold.Families[0], $sl.c)
  $sl.o
}
