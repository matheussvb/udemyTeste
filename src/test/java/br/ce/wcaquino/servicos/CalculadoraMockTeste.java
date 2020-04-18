package br.ce.wcaquino.servicos;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CalculadoraMockTeste {

	@Mock
	Calculadora calcMock;

	@Mock
	private EmailService emailServiceSpy;

	@Spy // não funciona com interface
	Calculadora calcSpy; // não sei o que fazer então faz a execução do método

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void deveMostrarDiferencaEntreMockeSpy() {

//		Mockito.when(calcMock.somar(1, 2)).thenCallRealMethod();
		Mockito.when(calcMock.somar(1, 2)).thenReturn(5);
//		Mockito.when(calcSpy.somar(1, 2)).thenReturn(5);
		Mockito.doReturn(5).when(calcSpy).somar(1, 2);
		Mockito.doNothing().when(calcSpy).imprimir();

		System.out.println("calcMoc: " + calcMock.somar(1, 2));
		System.out.println("calcSpy: " + calcSpy.somar(1, 2));

		System.out.println("MOck: ");
		calcMock.imprimir();
		System.out.println("Spyey: ");
		calcSpy.imprimir();
	}

	@Test
	public void teste() {

		ArgumentCaptor<Integer> argCapt = ArgumentCaptor.forClass(Integer.class);

		// Mockito.when(calc.somar(Mockito.anyInt(), Mockito.anyInt())).thenReturn(5);
		Mockito.when(calcMock.somar(argCapt.capture(), argCapt.capture())).thenReturn(5);

		Assert.assertEquals(5, calcMock.somar(1, 2));

//		System.out.println(argCapt.getAllValues());

	}

}
