param([string]$OutDir)

Add-Type -AssemblyName System.Drawing
$ref = @("System.Drawing")
Add-Type -ReferencedAssemblies $ref -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;
using System.Runtime.InteropServices;

public static class Art {
    static readonly Color Cream = Color.FromArgb(253, 244, 233);

    // Removes the flat cream backdrop, keeping anti-aliased edges and the soft drop shadow by
    // un-blending each pixel against the known background colour.
    public static Bitmap KeyOutCream(Bitmap src) {
        var outBmp = new Bitmap(src.Width, src.Height, PixelFormat.Format32bppArgb);
        var r = new Rectangle(0, 0, src.Width, src.Height);
        var sd = src.LockBits(r, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var od = outBmp.LockBits(r, ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        int n = src.Width * src.Height * 4;
        byte[] s = new byte[n]; byte[] o = new byte[n];
        Marshal.Copy(sd.Scan0, s, 0, n);
        for (int i = 0; i < n; i += 4) {
            int b = s[i], g = s[i + 1], rr = s[i + 2];
            int dev = Math.Max(Math.Abs(rr - Cream.R), Math.Max(Math.Abs(g - Cream.G), Math.Abs(b - Cream.B)));
            double a = Math.Min(1.0, dev / 38.0);
            if (a < 0.02) { o[i + 3] = 0; continue; }
            Func<int, int, int> un = (p, bg) => Math.Max(0, Math.Min(255, (int)Math.Round((p - (1 - a) * bg) / a)));
            o[i] = (byte)un(b, Cream.B); o[i + 1] = (byte)un(g, Cream.G); o[i + 2] = (byte)un(rr, Cream.R);
            o[i + 3] = (byte)(a * 255);
        }
        Marshal.Copy(o, 0, od.Scan0, n);
        src.UnlockBits(sd); outBmp.UnlockBits(od);
        return outBmp;
    }

    static void Glow(Graphics g, float cx, float cy, float rx, float ry, Color c, int alpha) {
        using (var path = new GraphicsPath()) {
            path.AddEllipse(cx - rx, cy - ry, rx * 2, ry * 2);
            using (var br = new PathGradientBrush(path)) {
                br.CenterColor = Color.FromArgb(alpha, c);
                br.SurroundColors = new[] { Color.FromArgb(0, c) };
                br.FocusScales = new PointF(0.15f, 0.15f);
                g.FillPath(br, path);
            }
        }
    }

    static GraphicsPath RoundRect(RectangleF r, float rad) {
        var p = new GraphicsPath(); float d = rad * 2;
        p.AddArc(r.X, r.Y, d, d, 180, 90); p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
        p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90); p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
        p.CloseFigure(); return p;
    }

    // 512x512 store icon: full-bleed square (Play applies its own rounded mask), cat centred at
    // ~66% width, with a faint warm vignette for depth.
    public static void Icon(Bitmap src, float catCx, float catCy, float catW, string path) {
        const int S = 512;
        using (var bmp = new Bitmap(S, S, PixelFormat.Format32bppArgb))
        using (var g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.Clear(Cream);
            float scale = (S * 0.66f) / catW;
            float dx = S / 2f - catCx * scale, dy = S / 2f - catCy * scale;
            g.DrawImage(src, dx, dy, src.Width * scale, src.Height * scale);
            using (var vig = new GraphicsPath()) {
                vig.AddEllipse(-S * 0.25f, -S * 0.25f, S * 1.5f, S * 1.5f);
                using (var br = new PathGradientBrush(vig)) {
                    br.CenterColor = Color.FromArgb(0, 240, 214, 190);
                    br.SurroundColors = new[] { Color.FromArgb(70, 236, 206, 180) };
                    br.FocusScales = new PointF(0.55f, 0.55f);
                    g.FillRectangle(br, 0, 0, S, S);
                }
            }
            bmp.Save(path, ImageFormat.Png);
        }
    }

    public static void Feature(Bitmap cat, RectangleF catBox, FontFamily black, FontFamily xbold, string path) {
        const int W = 1024, H = 500;
        using (var bmp = new Bitmap(W, H, PixelFormat.Format24bppRgb))
        using (var g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.TextRenderingHint = TextRenderingHint.AntiAlias;

            using (var bg = new LinearGradientBrush(new Rectangle(0, 0, W, H), Color.FromArgb(253, 244, 233), Color.FromArgb(236, 226, 255), 20f)) {
                var blend = new ColorBlend(3);
                blend.Colors = new[] { Color.FromArgb(253, 244, 233), Color.FromArgb(255, 226, 238), Color.FromArgb(234, 224, 255) };
                blend.Positions = new[] { 0f, 0.55f, 1f };
                bg.InterpolationColors = blend;
                g.FillRectangle(bg, 0, 0, W, H);
            }
            Glow(g, 250, 270, 290, 250, Color.FromArgb(255, 196, 120), 120);
            Glow(g, 930, 40, 260, 200, Color.FromArgb(196, 176, 255), 110);
            Glow(g, 700, 520, 240, 150, Color.FromArgb(255, 133, 162), 70);

            // sparkle dots
            using (var dot = new SolidBrush(Color.FromArgb(150, 255, 255, 255))) {
                foreach (var p in new[] { new PointF(92, 96), new PointF(430, 70), new PointF(470, 420), new PointF(60, 400), new PointF(980, 300) }) {
                    g.FillEllipse(dot, p.X - 5, p.Y - 5, 10, 10);
                }
            }

            float targetH = 350f;
            float scale = targetH / catBox.Height;
            float cx = 250, cy = 258;
            float dx = cx - (catBox.X + catBox.Width / 2) * scale;
            float dy = cy - (catBox.Y + catBox.Height / 2) * scale;
            g.DrawImage(cat, dx, dy, cat.Width * scale, cat.Height * scale);

            float tx = 492;
            var fmt = StringFormat.GenericTypographic;

            using (var word = new GraphicsPath()) {
                word.AddString("Vivichi", black, (int)FontStyle.Regular, 132f, new PointF(tx - 6, 70), fmt);
                var wb = word.GetBounds();
                using (var shadow = (GraphicsPath)word.Clone())
                using (var m = new Matrix()) {
                    m.Translate(0, 5); shadow.Transform(m);
                    using (var sb = new SolidBrush(Color.FromArgb(40, 120, 40, 80))) g.FillPath(sb, shadow);
                }
                using (var wg = new LinearGradientBrush(new RectangleF(wb.X, wb.Y, wb.Width, wb.Height), Color.FromArgb(242, 111, 138), Color.FromArgb(122, 46, 92), 25f)) {
                    g.FillPath(wg, word);
                }
            }

            using (var tag = new Font(xbold, 38f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (var tb = new SolidBrush(Color.FromArgb(61, 46, 78))) {
                g.DrawString("Keep your pet alive,", tag, tb, new PointF(tx, 232), fmt);
                g.DrawString("one habit at a time.", tag, tb, new PointF(tx, 280), fmt);
            }

            using (var chipFont = new Font(xbold, 21f, FontStyle.Regular, GraphicsUnit.Pixel))
            using (var chipText = new SolidBrush(Color.FromArgb(214, 84, 116)))
            using (var chipBg = new SolidBrush(Color.FromArgb(215, 255, 255, 255)))
            using (var chipDot = new SolidBrush(Color.FromArgb(255, 133, 162))) {
                float x = tx, y = 362;
                foreach (var label in new[] { "Daily habits", "Streaks", "Pet care" }) {
                    var sz = g.MeasureString(label, chipFont, 400, fmt);
                    float w = sz.Width + 50, h = 44;
                    using (var rr = RoundRect(new RectangleF(x, y, w, h), h / 2)) g.FillPath(chipBg, rr);
                    g.FillEllipse(chipDot, x + 17, y + h / 2 - 5, 10, 10);
                    g.DrawString(label, chipFont, chipText, new PointF(x + 34, y + (h - sz.Height) / 2 - 1), fmt);
                    x += w + 12;
                }
            }
            bmp.Save(path, ImageFormat.Png);
        }
    }
}
"@

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$src = [System.Drawing.Bitmap]::FromFile("C:\claude code\VivichiAndroid\app\src\main\res\drawable-xxxhdpi\ic_launcher_foreground.png")
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# cat bounding box measured from the source: x 154-466, y 198-496
[Art]::Icon($src, 310, 347, 312, (Join-Path $OutDir "icon-512.png"))

$pfcBlack = New-Object System.Drawing.Text.PrivateFontCollection
$pfcBlack.AddFontFile((Join-Path $dir "Nunito-Black.ttf"))
$pfcX = New-Object System.Drawing.Text.PrivateFontCollection
$pfcX.AddFontFile((Join-Path $dir "Nunito-ExtraBold.ttf"))
"fonts: " + $pfcBlack.Families[0].Name + " / " + $pfcX.Families[0].Name

$cat = [Art]::KeyOutCream($src)
$box = New-Object System.Drawing.RectangleF(150, 194, 320, 312)
[Art]::Feature($cat, $box, $pfcBlack.Families[0], $pfcX.Families[0], (Join-Path $OutDir "feature-graphic-1024x500.png"))

$cat.Dispose(); $src.Dispose()
Get-ChildItem $OutDir | ForEach-Object {
  $i = [System.Drawing.Image]::FromFile($_.FullName)
  "{0}  {1}x{2}  {3}  {4:N0} KB" -f $_.Name, $i.Width, $i.Height, $i.PixelFormat, ($_.Length / 1KB)
  $i.Dispose()
}
