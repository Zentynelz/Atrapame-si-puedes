@echo off
title Instalador de Dependencias - Atrapame Si Puedes
echo =======================================================
echo    INSTALADOR DE DEPENDENCIAS PYTHON
echo    (Ejecuta este archivo UNA SOLA VEZ)
echo =======================================================
echo.

cd /d "%~dp0"

echo Verificando Python...
python --version
if %errorlevel% neq 0 (
    echo ERROR: Python no esta instalado.
    echo Descargalo de https://www.python.org/downloads/
    echo Asegurate de marcar "Add Python to PATH" al instalar.
    pause
    exit /b 1
)

echo.
echo Instalando librerias necesarias...
pip install matplotlib seaborn pandas numpy

echo.
echo =======================================================
echo  INSTALACION COMPLETA.
echo  Ahora puedes usar Descargar_Resultados.bat
echo.
echo  RECUERDA: Necesitas el archivo google-services.json
echo  dentro de la carpeta "app\" (pidele a Diego que te lo
echo  mande por WhatsApp).
echo =======================================================
pause
