Add-Type -AssemblyName PresentationCore,WindowsBase
$root=Join-Path $PSScriptRoot '..\..\assets'
$tones=@('#303A61','#29443F','#493554','#543B37','#223049')
for($i=0;$i -lt $tones.Count;$i++){
 $visual=New-Object Windows.Media.DrawingVisual
 $dc=$visual.RenderOpen()
 $bg=[Windows.Media.BrushConverter]::new().ConvertFromString($tones[$i])
 $dc.DrawRectangle($bg,$null,(New-Object Windows.Rect 0,0,256,256))
 $dc.PushOpacity(0.16)
 $dc.DrawEllipse([Windows.Media.Brushes]::White,$null,(New-Object Windows.Point (218-$i*12),36),132,132)
 $dc.DrawEllipse([Windows.Media.Brushes]::White,$null,(New-Object Windows.Point 30,236),100,100)
 $dc.Pop()
 $ink=[Windows.Media.BrushConverter]::new().ConvertFromString('#0B101B')
 $light=[Windows.Media.BrushConverter]::new().ConvertFromString('#C9DFFF')
 $dc.DrawEllipse($ink,$null,(New-Object Windows.Point 144,140),76,76)
 $pen=New-Object Windows.Media.Pen $bg,2
 $dc.DrawEllipse($null,$pen,(New-Object Windows.Point 144,140),60,60)
 $dc.DrawEllipse($null,$pen,(New-Object Windows.Point 144,140),48,48)
 $dc.DrawEllipse($light,$null,(New-Object Windows.Point 144,140),26,26)
 $dc.DrawEllipse($ink,$null,(New-Object Windows.Point 144,140),6,6)
 $dc.Close()
 $bitmap=New-Object Windows.Media.Imaging.RenderTargetBitmap 256,256,96,96,([Windows.Media.PixelFormats]::Pbgra32)
 $bitmap.Render($visual)
 $encoder=New-Object Windows.Media.Imaging.PngBitmapEncoder
 $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
 $stream=[IO.File]::Create((Join-Path $root "studio-cover-$i.png"))
 try{$encoder.Save($stream)}finally{$stream.Dispose()}
}
