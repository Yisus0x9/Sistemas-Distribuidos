;/**@brief ESTE PROGRAMA MUESTRA LOS BLOQUES QUE FORMAN UN PROGRAMA 
; * EN ENSAMBLADOR, LOS BLOQUES SON:
; * BLOQUE 1. OPCIONES DE CONFIGURACION DEL DSC: OSCILADOR, WATCHDOG,
; * BROWN OUT RESET, POWER ON RESET Y CODIGO DE PROTECCION
; * BLOQUE 2. EQUIVALENCIAS Y DECLARACIONES GLOBALES
; * BLOQUE 3. ESPACIOS DE MEMORIA: PROGRAMA, DATOS X, DATOS Y, DATOS NEAR
; * BLOQUE 4. CÓDIGO DE APLICACIÓN
; * @device: DSPIC30F4013
; * @oscilLator: FRC, 7.3728MHz
; */
        .equ __30F4013, 1
        .include "p30F4013.inc"
;**************************
; BITS DE CONFIGURACIÓN
;**************************
;..............................................................................
;SE DESACTIVA EL CLOCK SWITCHING Y EL FAIL-SAFE CLOCK MONITOR (FSCM) Y SE 
;ACTIVA EL OSCILADOR INTERNO DE 7.3728MHZ(FAST RC) PARA TRABAJAR
;FSCM: PERMITE AL DISPOSITIVO CONTINUAR OPERANDO AUN CUANDO OCURRA UNA FALLA 
;EN EL OSCILADOR. CUANDO OCURRE UNA FALLA EN EL OSCILADOR SE GENERA UNA TRAMPA
;Y SE CAMBIA EL RELOJ AL OSCILADOR FRC  
;..............................................................................
        config __FOSC, CSW_FSCM_OFF & FRC   
;..............................................................................
;SE DESACTIVA EL WATCHDOG
;..............................................................................
        config __FWDT, WDT_OFF 
;..............................................................................
;SE ACTIVA EL POWER ON RESET (POR), BROWN OUT RESET (BOR), POWER UP TIMER (PWRT)
;Y EL MASTER CLEAR (MCLR)
;POR: AL MOMENTO DE ALIMENTAR EL DSPIC OCURRE UN RESET CUANDO EL VOLTAJE DE 
;ALIMENTACIÓN ALCANZA UN VOLTAJE DE UMBRAL (VPOR), EL CUAL ES 1.85V
;BOR: ESTE MODULO GENERA UN RESET CUANDO EL VOLTAJE DE ALIMENTACIÓN DECAE
;POR DEBAJO DE UN CIERTO UMBRAL ESTABLECIDO (2.7V) 
;PWRT: MANTIENE AL DSPIC EN RESET POR UN CIERTO TIEMPO ESTABLECIDO, ESTO AYUDA
;A ASEGURAR QUE EL VOLTAJE DE ALIMENTACIÓN SE HA ESTABILIZADO (16ms) 
;..............................................................................
        config __FBORPOR, PBOR_ON & BORV27 & PWRT_16 & MCLR_EN
;..............................................................................
;SE DESACTIVA EL CÓDIGO DE PROTECCIÓN
;..............................................................................
   	config __FGS, CODE_PROT_OFF & GWRP_OFF      

;**************************
; SECCIÓN DE DECLARACIÓN DE CONSTANTES CON LA DIRECTIVA .EQU (= DEFINE EN C)
;**************************
        .equ MUESTRAS, 64         ;NÚMERO DE MUESTRAS

;**************************
; DECLARACIONES GLOBALES
;**************************
;..............................................................................
;PROPORCIONA ALCANCE GLOBAL A LA FUNCIÓN _wreg_init, ESTO PERMITE LLAMAR A LA 
;FUNCIÓN DESDE UN OTRO PROGRAMA EN ENSAMBLADOR O EN C COLOCANDO LA DECLARACIÓN
;"EXTERN"
;..............................................................................
        .global _wreg_init     
;..............................................................................
;ETIQUETA DE LA PRIMER LINEA DE CÓDIGO
;..............................................................................
        .global __reset          
;..............................................................................
;DECLARACIÓN DE LA ISR DEL TIMER 1 COMO GLOBAL
;..............................................................................
        .global __T1Interrupt    

;**************************
;CONSTANTES ALMACENADAS EN EL ESPACIO DE LA MEMORIA DE PROGRAMA
;**************************
        .section .myconstbuffer, code
;..............................................................................
;ALINEA LA SIGUIENTE PALABRA ALMACENADA EN LA MEMORIA 
;DE PROGRAMA A UNA DIRECCION MULTIPLO DE 2
;..............................................................................
        .palign 2                

ps_coeff:
        .hword   0x0002, 0x0003, 0x0005, 0x000A

;**************************
;VARIABLES NO INICIALIZADAS EN EL ESPACIO X DE LA MEMORIA DE DATOS
;**************************
         .section .xbss, bss, xmemory

x_input: .space 2*MUESTRAS        ;RESERVANDO ESPACIO (EN BYTES) A LA VARIABLE

;**************************
;VARIABLES NO INICIALIZADAS EN EL ESPACIO Y DE LA MEMORIA DE DATOS
;**************************
          .section .ybss, bss, ymemory

y_input:  .space 2*MUESTRAS       ;RESERVANDO ESPACIO (EN BYTES) A LA VARIABLE

;**************************
;VARIABLES NO INICIALIZADAS LA MEMORIA DE DATOS CERCANA (NEAR), LOCALIZADA
;EN LOS PRIMEROS 8KB DE RAM
;**************************
          .section .nbss, bss, near

var1:     .space 2               ;LA VARIABLE VAR1 RESERVA 1 WORD DE ESPACIO

;**************************
;SECCION DE CODIGO EN LA MEMORIA DE PROGRAMA
;**************************
.text					;INICIO DE LA SECCION DE CODIGO

__reset:
        MOV	#__SP_init, 	W15	;INICIALIZA EL STACK POINTER

        MOV 	#__SPLIM_init, 	W0     	;INICIALIZA EL REGISTRO STACK POINTER LIMIT 
        MOV 	W0, 		SPLIM

        NOP                       	;UN NOP DESPUES DE LA INICIALIZACION DE SPLIM

        CALL 	_WREG_INIT          	;SE LLAMA A LA RUTINA DE INICIALIZACION DE REGISTROS
        CALL    INI_PERIFERICOS
	
        ; Habilitar interrupciones globales
        BSET    INTCON1, #NSTDIS    ; Deshabilitar interrupciones anidadas
        BSET    SR, #IPL0           ; Habilitar interrupciones nivel CPU 0
	
        CLR	W0
	    
CICLO:
	MOV	PORTB,	W0
	BTSC	W0,	#0
	CALL    DO
	BTSC	W0,	#1
	CALL    RE
	BTSC	W0,	#2
	CALL    MI
	BTSC	W0,	#3
	CALL    FA
	BTSC	W0,	#4
	CALL    SOL
	BTSC	W0,	#5
	CALL    LA
	BTSC	W0,	#6
	CALL    SI
	MOV	W0,	PORTF
	GOTO    CICLO

	; Agregar verificación del botón
	BTSC	W0, #0          ; Verifica si el botón está presionado
	GOTO    CICLO          ; Si está presionado, continúa el ciclo
	; Si no está presionado, detener el sonido
	CLR     TMR1           ; Detener el Timer1
	GOTO    CICLO          ; Regresar al ciclo
	
DO:
	CLR	TMR1
	MOV	#0x0DC2,    W0      ; Periodo para la nota DO
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
        ; Configurar LED RGB - Rojo
	BSET    LATF,       #0     ; Enciende Rojo (RF0)
	BCLR    LATF,       #1     ; Apaga Verde (RF1)
	BCLR    LATF,       #2     ; Apaga Azul (RF2)
        RETURN
	
RE:
	CLR	TMR1
	MOV	#0x0C42,    W0      ; Periodo para la nota RE
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
        ; Configurar LED RGB - Verde
	BCLR    LATF,       #0     
	BSET    LATF,       #1     
	BCLR    LATF,       #2     
        RETURN

MI:
	CLR	TMR1
	MOV	#0x0AEC,    W0      ; Periodo para la nota MI
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
        ; Configurar LED RGB - Azul
	BCLR    LATF,       #0     
	BCLR    LATF,       #1     
	BSET    LATF,       #2     
        RETURN

FA:
	CLR	TMR1
	MOV	#0x0A4F,    W0      ; Periodo para la nota FA
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
        ; Configurar LED RGB - Amarillo
	BSET    LATF,       #0     
	BSET    LATF,       #1     
	BCLR    LATF,       #2     
        RETURN

SOL:
	CLR	TMR1
	MOV	#0x092F,    W0      ; Periodo para la nota SOL
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
        ; Configurar LED RGB - Púrpura
	BSET    LATF,       #0     
	BCLR    LATF,       #1     
	BSET    LATF,       #2     
        RETURN

LA:
	CLR	TMR1
	MOV	#0x082F,    W0      ; Periodo para la nota LA
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
	; Configurar LED RGB - Cian
	BCLR    LATF,       #0     
	BSET    LATF,       #1     
	BSET    LATF,       #2     
        RETURN

SI:
	CLR	TMR1
	MOV	#0x074A,    W0      ; Periodo para la nota SI
	MOV	W0,	    PR1
	
        ; Configuración del Timer1
	MOV	#0x8000,    W0      ; TGATE=0, TCKPS=00 (1:1), TSYNC=0
        MOV     W0,         T1CON   ; TSIDL=0, TCS=0 (Internal clock)
	
        ; Configuración de la interrupción
	BCLR	IFS0,	    #T1IF   ; Limpiar flag de interrupción
	BSET	IEC0,	    #T1IE   ; Habilitar interrupción Timer1
        
        ; Configurar prioridad de la interrupción
	BSET	IPC0,	    #T1IP0
	BSET	IPC0,	    #T1IP1
	BSET	IPC0,	    #T1IP2
	
        BSET    T1CON,	    #TON    ; Iniciar Timer1
	BSET    LATF,       #4
	
        ; Configurar LED RGB - Blanco
	BSET    LATF,       #0     
	BSET    LATF,       #1     
	BSET    LATF,       #2     
        RETURN

;/**@brief ESTA RUTINA INICIALIZA LOS PERIFERICOS DEL DSC
; */
INI_PERIFERICOS:
	CLR	PORTF	    ;PORTF = 0
	NOP
	CLR	LATF	    ;LATF = 0
	NOP
	CLR	TRISF	    ;TRISF = 0 salida
	NOP
	
	CLR	PORTB	    ;PORTB = 0
	NOP
	CLR	LATB	    ;LATB = 0
	NOP
	SETM	TRISB	    ;TRISB = 0xFFFF entrada
	NOP
	SETM	ADPCFG	    ;ADPCFG = 0XFFFF
			    ;SE DESHABILITA EL ADC
        RETURN

;/**@brief ESTA RUTINA INICIALIZA LOS REGISTROS Wn A 0X0000
; */
_WREG_INIT:
        CLR 	W0
        MOV 	W0, 		W14
        REPEAT 	#12
        MOV 	W0, 		[++W14]
        CLR 	W14
        RETURN

;/**@brief ISR (INTERRUPT SERVICE ROUTINE) DEL TIMER 1 
; * Esta rutina se ejecuta cada vez que ocurre una interrupción del Timer 1
; */
__T1Interrupt:
        PUSH.S              ; Guardar registros en stack

        BCLR    IFS0, #T1IF ; Limpiar la bandera de interrupción del Timer 1
        BTG     LATF, #6    ; Toggle del pin RF6 (alterna el estado)

        POP.S              ; Recuperar registros del stack
        RETFIE            ; Retorno de interrupción

.END
