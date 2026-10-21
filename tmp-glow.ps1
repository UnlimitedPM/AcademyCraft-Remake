Add-Type -AssemblyName System.Drawing
foreach ($n in @('glow_left', 'glow_right', 'glow_up', 'glow_ru')) {
    $p = "c:\Users\Paul\Desktop\Dev\AcademyCraft\src\main\resources\assets\academy\textures\guis\$n.png"
    $img = [System.Drawing.Bitmap]::FromFile($p)
    $alphaMin = 255; $alphaMax = 0
    for ($y = 0; $y -lt $img.Height; $y += 2) {
        for ($x = 0; $x -lt $img.Width; $x += 2) {
            $a = $img.GetPixel($x, $y).A
            if ($a -lt $alphaMin) { $alphaMin = $a }
            if ($a -gt $alphaMax) { $alphaMax = $a }
        }
    }
    Write-Output ($n + ' : ' + $img.Width + 'x' + $img.Height + ' alpha ' + $alphaMin + '..' + $alphaMax)
    # profil d'alpha sur la ligne du milieu, colonne par colonne
    $row = [Math]::Floor($img.Height / 2)
    $profile = @()
    for ($x = 0; $x -lt $img.Width; $x++) { $profile += $img.GetPixel($x, $row).A }
    Write-Output ('   alpha au milieu : ' + ($profile -join ','))
    $img.Dispose()
}
