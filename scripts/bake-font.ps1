# Genere les planches de glyphes de la police de l'interface.
#
# POURQUOI CE SCRIPT EXISTE
#
# Minecraft ne sait pas dessiner une police TTF proprement : il la rasterise *sans hinting*
# (contrairement a Windows, qui hint) puis la REDUIT, et cette reduction abime le trait — au
# plus proche voisin elle le cisaille, en lisse elle le noie. Aucun reglage de taille, de
# sur-echantillonnage ou de filtrage n'y change quoi que ce soit : verifie a l'ecran, puis
# reproduit hors du jeu en reduisant la meme image des deux facons.
#
# La seule issue est donc de ne plus laisser Minecraft rasteriser. Ce script pre-rend les
# glyphes lui-meme, AVEC HINTING et a chaque taille ou le mod les dessine, dans une planche
# PNG par taille. Minecraft les dessine alors pixel pour pixel : net et plein. Et cela marche
# avec n'importe quelle police — Windows ou libre — puisque c'est le pre-rendu qui apporte la
# nettete, pas la provenance de la police.
#
# LE FORMAT
#
# Celui du fournisseur « bitmap » de Minecraft : une grille de cases, une case par caractere,
# la hauteur de ligne dans `height`, la ligne de base dans `ascent`, et une rangee de `chars`
# par rangee de la grille. L'avance d'un glyphe n'est PAS la largeur de sa case : Minecraft
# mesure son ENCRE (la derniere colonne non transparente) et ajoute un pixel. C'est ce qui rend
# une police proportionnelle possible, et sur une police serree comme YaHei, encre + 1 tombe
# justement sur l'avance naturelle : les largeurs ne bougent pas.
#
# Un caractere sans encre n'avancerait que d'un pixel : l'espace recoit donc un pixel a alpha 1,
# invisible mais bien mesure.
#
# Usage : powershell -File scripts/bake-font.ps1 [-Source police.ttc] [-OutDir dossier] [-TexDir dossier]

param(
    [string]$Source = "$env:WINDIR\Fonts\msyh.ttc",
    [string]$OutDir = "src/main/resources/assets/academy/font",
    [string]$TexDir = "src/main/resources/assets/academy/textures/font"
)

Add-Type -AssemblyName System.Drawing

# Les tailles couvertes : le mod dessine son interface entre 8 et 13 pixels, et les titres des
# autres ecrans montent plus haut. Une planche par taille entiere.
$Sizes = 7..20

# La grille : 32 cases par rangee.
$Columns = 32

# Ce qu'on grave : l'ASCII imprimable, le latin accentue (le mod est traduit en francais), et
# quelques signes typographiques. Le chinois retombe sur la police du jeu : une planche de CJK
# a chaque taille serait enorme pour un besoin qui n'existe pas encore.
$Codes = @()
$Codes += 32..126
$Codes += 160..255
$Codes += 0x2018, 0x2019, 0x201C, 0x201D, 0x2013, 0x2014, 0x2026, 0x00B0, 0x00B2, 0x00B3, 0x20AC

function Escape-Json([string]$Text) {
    $Builder = New-Object System.Text.StringBuilder
    foreach ($Char in $Text.ToCharArray()) {
        $Code = [int]$Char
        if ($Char -eq '"') { [void]$Builder.Append('\"') }
        elseif ($Char -eq '\') { [void]$Builder.Append('\\') }
        elseif ($Code -lt 32 -or $Code -gt 126) { [void]$Builder.Append(('\u{0:x4}' -f $Code)) }
        else { [void]$Builder.Append($Char) }
    }
    return $Builder.ToString()
}

if (-not (Test-Path $Source)) { throw "police introuvable : $Source" }
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }
if (-not (Test-Path $TexDir)) { New-Item -ItemType Directory -Path $TexDir -Force | Out-Null }

$Collection = New-Object System.Drawing.Text.PrivateFontCollection
$Collection.AddFontFile($Source)
$Family = $Collection.Families[0]
$Style = [System.Drawing.FontStyle]::Regular
$Format = [System.Drawing.StringFormat]::GenericTypographic

"famille : $($Family.Name)"

foreach ($Size in $Sizes) {
    $Font = New-Object System.Drawing.Font($Family, $Size, $Style, [System.Drawing.GraphicsUnit]::Pixel)
    $Height = [int][Math]::Round($Font.GetHeight())
    $Ascent = [int][Math]::Round($Family.GetCellAscent($Style) / $Family.GetEmHeight($Style) * $Size)

    # La case doit tenir le glyphe le plus large, encre comprise.
    $Probe = New-Object System.Drawing.Bitmap(1, 1)
    $ProbeGraphics = [System.Drawing.Graphics]::FromImage($Probe)
    $ProbeGraphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $Width = 1
    foreach ($Code in $Codes) {
        if ($Code -eq 32) { continue }
        $Measured = $ProbeGraphics.MeasureString([string][char]$Code, $Font,
            (New-Object System.Drawing.PointF(0, 0)), $Format)
        if ($Measured.Width -gt $Width) { $Width = [int][Math]::Ceiling($Measured.Width) }
    }
    $ProbeGraphics.Dispose()
    $Probe.Dispose()
    $Width = $Width + 1

    $Rows = [int][Math]::Ceiling($Codes.Count / $Columns)
    $Sheet = New-Object System.Drawing.Bitmap(($Columns * $Width), ($Rows * $Height),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $Graphics = [System.Drawing.Graphics]::FromImage($Sheet)
    $Graphics.Clear([System.Drawing.Color]::FromArgb(0, 255, 255, 255))
    # AntiAliasGridFit : c'est le rendu HINTE de Windows. Sans lui, les traits fins s'effacent
    # a cette taille, exactement comme dans Minecraft (PIL, par exemple, ne hint pas).
    $Graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $Graphics.PageUnit = [System.Drawing.GraphicsUnit]::Pixel

    $Lines = @()
    for ($Row = 0; $Row -lt $Rows; $Row++) {
        $Line = New-Object System.Text.StringBuilder
        for ($Column = 0; $Column -lt $Columns; $Column++) {
            $Index = $Row * $Columns + $Column
            if ($Index -ge $Codes.Count) {
                [void]$Line.Append([char]0)
                continue
            }

            $Code = $Codes[$Index]
            [void]$Line.Append([char]$Code)

            $X = $Column * $Width
            $Baseline = $Row * $Height + $Ascent

            if ($Code -eq 32) {
                # L'espace n'a pas d'encre : un pixel a alpha 1 lui donne son avance, sans rien
                # laisser voir.
                $Sheet.SetPixel(($X + [Math]::Max(1, [int]($Size / 3))), ($Baseline - 1),
                    [System.Drawing.Color]::FromArgb(1, 255, 255, 255))
                continue
            }

            # La chaine se pose par le HAUT de sa ligne : on remonte donc de l'ascendante pour
            # que la ligne de base tombe juste.
            $Graphics.DrawString([string][char]$Code, $Font, [System.Drawing.Brushes]::White,
                $X, ($Baseline - $Ascent), $Format)
        }
        $Lines += $Line.ToString()
    }
    $Graphics.Dispose()

    $Name = "ac_gui_$Size"

    # Les DEUX endroits ne sont pas interchangeables : la definition se lit dans `font/`, mais
    # l'image d'un fournisseur « bitmap » se lit dans `textures/font/` — Minecraft prefixe lui-meme
    # le chemin de `file` par `textures/`, donc une planche rangee dans `font/` est introuvable et
    # le fournisseur est rejete (les glyphes manquent alors TOUS, sans autre message qu'un
    # avertissement dans le journal).
    $Sheet.Save((Join-Path $TexDir "$Name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $Sheet.Dispose()
    $Font.Dispose()

    # Le repli sur la police du jeu n'est pas un ornement : si la planche manque (depot
    # fraichement clone, police non regeneree), seule cette definition-la tombe, et le texte
    # reste lisible au lieu de disparaitre.
    $Chars = ($Lines | ForEach-Object { '        "' + (Escape-Json $_) + '"' }) -join ",`n"
    $Json = @(
        '{',
        '  "providers": [',
        '    {',
        '      "type": "bitmap",',
        "      `"file`": `"academy:font/$Name.png`",",
        "      `"height`": $Height,",
        "      `"ascent`": $Ascent,",
        '      "chars": [',
        $Chars,
        '      ]',
        '    },',
        '    { "type": "reference", "id": "minecraft:default" }',
        '  ]',
        '}'
    ) -join "`n"
    Set-Content -Path (Join-Path $OutDir "$Name.json") -Value $Json -Encoding ascii

    "$Name : $($Codes.Count) caracteres, cases de ${Width}x${Height}, ascendante $Ascent"
}

"planches ecrites dans $TexDir et definitions dans $OutDir (a partir de $Source)"
"rappel : les .ttf et les planches ne sont pas redistribuables, elles restent hors du depot"
"et se regenerent avec ce script."
