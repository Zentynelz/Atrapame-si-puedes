import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import argparse

def plot_stress_timeline(json_file_path, output_image_path):
    # 1. Cargar el JSON de Firebase
    with open(json_file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # El array de emotionTimeline
    timeline = data.get("emotionTimeline", [])
    if not timeline:
        print("Error: No se encontró 'emotionTimeline' en el JSON.")
        return

    # Convertir a DataFrame de Pandas para fácil manejo
    df = pd.DataFrame(timeline)
    
    # Asegurar que los datos están ordenados por tiempo
    df.sort_values(by="timeMs", inplace=True)
    
    # Convertir milisegundos a segundos para el eje X
    df['timeSec'] = df['timeMs'] / 1000.0

    # 2. Configurar estilo académico (para LaTeX / Artículo)
    sns.set_theme(style="whitegrid", context="paper")
    plt.rcParams.update({
        'font.size': 12,
        'axes.labelsize': 12,
        'axes.titlesize': 14,
        'legend.fontsize': 10,
        'figure.figsize': (10, 5),
        'figure.dpi': 300
    })

    # 3. Crear la gráfica
    fig, ax1 = plt.subplots()

    # Línea principal: Nivel de Estrés vs Tiempo
    color_stress = 'tab:red'
    ax1.set_xlabel('Tiempo de Partida (Segundos)')
    ax1.set_ylabel('Nivel de Estrés (0-100)', color=color_stress, fontweight='bold')
    line1, = ax1.plot(df['timeSec'], df['stressLevel'], color=color_stress, linewidth=2.5, label='Nivel de Estrés')
    ax1.tick_params(axis='y', labelcolor=color_stress)
    ax1.set_ylim(-5, 105)

    # Rellenar bajo la curva paramostrar intensidad
    ax1.fill_between(df['timeSec'], df['stressLevel'], alpha=0.2, color=color_stress)

    # Eje Secundario (Opcional): Dificultad o Velocidad del Enemigo
    ax2 = ax1.twinx()
    color_diff = 'tab:blue'
    ax2.set_ylabel('Dificultad Dinámica (Retraso Enemigo ms)', color=color_diff)
    line2, = ax2.plot(df['timeSec'], df['difficultyDelayMs'], color=color_diff, linestyle='--', linewidth=2, label='Velocidad IA')
    ax2.tick_params(axis='y', labelcolor=color_diff)
    
    # Invertir eje Y si se desea (menor retraso = mayor dificultad)
    ax2.invert_yaxis()

    # Añadir marcadores donde hubo "Habla/Grito" detectado
    speech_df = df[df['isSpeech'] == True]
    if not speech_df.empty:
        scatter = ax1.scatter(speech_df['timeSec'], speech_df['stressLevel'], 
                              color='darkred', marker='x', s=50, zorder=5, label='Voz/Estrés detectado')
        # Crear leyenda combinada
        lines = [line1, line2, scatter]
        labels = [l.get_label() for l in lines]
        ax1.legend(lines, labels, loc='upper left')
    else:
        lines = [line1, line2]
        labels = [l.get_label() for l in lines]
        ax1.legend(lines, labels, loc='upper left')

    # Título y ajustes finales
    plt.title('Modulación de Dificultad Dinámica (DDA) en base a Biofeedback', fontweight='bold')
    fig.tight_layout()

    # 4. Guardar archivo de imagen
    plt.savefig(output_image_path, format='png', bbox_inches='tight')
    plt.savefig(output_image_path.replace('.png', '.pdf'), format='pdf', bbox_inches='tight') # Formato PDF para LaTeX
    
    print(f"Graficas guardadas con exito en: {output_image_path} (y formato .pdf)!")
    plt.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generar gráfica de estrés a partir de JSON Firebase")
    parser.add_argument("--input", "-i", type=str, default="muestra_partida.json", help="Ruta al archivo JSON de entrada")
    parser.add_argument("--output", "-o", type=str, default="grafica_resultados.png", help="Nombre de imagen de salida")
    args = parser.parse_args()
    
    plot_stress_timeline(args.input, args.output)
