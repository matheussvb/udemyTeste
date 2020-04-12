package br.ce.wcaquino.servicos;

import org.junit.Test;
import org.mockito.Mockito;

public class CalculadoraMockTeste {

	@Test
	public void teste() {
		
		Calculadora calc = Mockito.mock(Calculadora.class);
		
		Mockito.when(calc.somar(Mockito.anyInt(), Mockito.anyInt())).thenReturn(5);
		System.out.println(calc.somar(1, 2));
		
		
	}
	
}
