# Room e Compose já trazem suas próprias regras de consumo.

# Widgets (Glance): cada widget precisa continuar sendo uma classe própria, com nome próprio.
# Sem isso, o R8 funde os quatro numa só classe e um widget passa a mostrar o conteúdo de outro.
-keep class br.app.relatoriocampo.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }
