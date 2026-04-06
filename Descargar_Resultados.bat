@echo off
title Descargar y Graficar Resultados Firebase
echo =======================================================
echo    EXTRACTOR DE BIOFEEDBACK - ATRAPAME SI PUEDES
echo =======================================================
echo.

:: Ir a la carpeta donde esta este .bat (lo hace funcionar con doble clic)
cd /d "%~dp0"

echo Carpeta de trabajo: %cd%
echo.
echo Comunicandose con la base de datos en la nube...
echo.

:: Ejecutar el script de extraccion de python
python organizar_firebase.py

echo.
echo =======================================================
echo  Proceso Finalizado. Presiona cualquier tecla para salir.
echo =======================================================
pause > nul
