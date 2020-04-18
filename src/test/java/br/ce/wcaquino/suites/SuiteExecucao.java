package br.ce.wcaquino.suites;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import br.ce.wcaquino.servicos.CalculadoraTest;
import br.ce.wcaquino.servicos.CalculoValorLocacaoTest;
import br.ce.wcaquino.servicos.LocacaoServiceTest;

//@RunWith(Suite.class) // roda tudo junto
@SuiteClasses({ 
//	CalculadoraTest.class,
	CalculoValorLocacaoTest.class, LocacaoServiceTest.class })
public class SuiteExecucao {

	// remova se puder

	@BeforeClass
	public static void before() {
		System.out.println("@BeforeClass");
	}

	@AfterClass
	public static void after() {
		System.out.println("@@AfterClass");
	}

}
