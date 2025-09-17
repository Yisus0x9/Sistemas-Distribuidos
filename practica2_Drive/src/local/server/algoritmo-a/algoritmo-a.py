import numpy as np
import heapq
import matplotlib.pyplot as plt

# Definir el mapa (10x10) 
# 0 es libre
# 1 es un obstaculo
mapa = np.array([
    [0, 0, 0, 0, 0, 0, 1, 0, 0, 0],
    [0, 1, 1, 1, 1, 0, 1, 0, 1, 0],
    [0, 0, 0, 0, 1, 0, 1, 0, 1, 0],
    [0, 1, 0, 1, 1, 0, 1, 0, 1, 0],
    [0, 1, 0, 1, 0, 0, 0, 0, 1, 0],
    [0, 1, 0, 1, 1, 1, 1, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 1, 1, 1, 0],
    [1, 1, 1, 1, 1, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 1, 0, 1, 1, 1, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 1, 0]
])

inicio = (0, 0)  # Punto de inicio (fila, columna)
objetivo = (9, 9)  # Punto objetivo (fila, columna)

'''
Funcion heuristica
 - Busqueda heuristica
 - Se mueve en lineas rectas o en angulo recto
'''
def heuristica_manhattan(nodo, objetivo):
    return abs(nodo[0] - objetivo[0]) + abs(nodo[1] - objetivo[1])

''' 
Implementacion del Algoritmo A*
 - busqueda informada
 - combina de manera efectiva costo del camino hasta el nodo actual con una 
   estimacion heuristica del costo hasta el objetivo
   
   f(n) = g(n)+h(n)
   g(n) = costo exacto desde el inicio hasta el nodo n
   h(n) = estimacion heurica del costo dese n hasta la meta 
 - completo y optimo

El algoritmo expora el espacioo de busqeda usando la funcion heuristica 
'''
def a_star_grid(mapa, inicio, objetivo):
    nfilas, ncols = mapa.shape
    g = np.full((nfilas, ncols), np.inf)
    f = np.full((nfilas, ncols), np.inf)
    g[inicio] = 0
    f[inicio] = heuristica_manhattan(inicio, objetivo)
    abierta = [(f[inicio], inicio)]
    predecesor = np.full((nfilas, ncols, 2), -1)
    movimientos = [(-1, 0), (1, 0), (0, -1), (0, 1)]
    
    while abierta:
        _, actual = heapq.heappop(abierta)
        if actual == objetivo:
            camino = []
            while actual != (-1, -1):
                camino.append(actual)
                actual = tuple(predecesor[actual])
            return camino[::-1], g[objetivo]
        
        for mov in movimientos:
            vecino = (actual[0] + mov[0], actual[1] + mov[1])
            if 0 <= vecino[0] < nfilas and 0 <= vecino[1] < ncols and mapa[vecino] == 0:
                tentativo_g = g[actual] + 1
                if tentativo_g < g[vecino]:
                    predecesor[vecino] = actual
                    g[vecino] = tentativo_g
                    f[vecino] = g[vecino] + heuristica_manhattan(vecino, objetivo)
                    heapq.heappush(abierta, (f[vecino], vecino))
    return [], np.inf

# Ejecutar el Algoritmo
camino, costo_total = a_star_grid(mapa, inicio, objetivo)
print("Camino encontrado:", camino)
print("Costo total:", costo_total)

# Visualizar el camino en el mapa
plt.imshow(mapa, cmap='gray_r')
plt.scatter(inicio[1], inicio[0], c='green', s=100, label='Inicio')
plt.scatter(objetivo[1], objetivo[0], c='red', s=100, label='Objetivo')
if camino:
    camino_x, camino_y = zip(*camino)
    plt.plot(camino_y, camino_x, 'b-', linewidth=2, label='Camino')
plt.legend()
plt.title("Camino encontrado por A*")
plt.show()
