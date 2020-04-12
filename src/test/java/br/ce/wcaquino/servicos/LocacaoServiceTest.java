package br.ce.wcaquino.servicos;

import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.FilmeBuilder.umFilmeSemEstoque;
import static br.ce.wcaquino.builders.LocacaoBuilder.umLocacao;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.DataDiferencaDiasMatchers.ehHoje;
import static br.ce.wcaquino.matchers.DataDiferencaDiasMatchers.ehHojeComDiferencaDias;
import static br.ce.wcaquino.matchers.MatchersProprios.caiEm;
import static br.ce.wcaquino.matchers.MatchersProprios.caiNumaSegunda;
import static br.ce.wcaquino.utils.DataUtils.isMesmaData;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import br.ce.wcaquino.dao.LocacaoDao;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.FilmeSemEstoqueException;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.utils.DataUtils;
import buildermaster.BuilderMaster;

public class LocacaoServiceTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public ErrorCollector erro = new ErrorCollector();

	private LocacaoService service;

	private SPCService spcService;

	private LocacaoDao dao;

	private EmailService emailService;

	@Before
	public void setUp() {
		System.out.println("@Before");

		service = new LocacaoService();

		dao = Mockito.mock(LocacaoDao.class);
		service.setLocacaoDao(dao);

		spcService = Mockito.mock(SPCService.class);
		service.setSPCService(spcService);

		emailService = Mockito.mock(EmailService.class);
		service.setEmailService(emailService);

	}

	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas() {
		// cenario
		Usuario usuario1 = umUsuario().agora();
		Usuario usuario2 = umUsuario().comNome("Usuario em dia").agora();
		Usuario usuario3 = umUsuario().comNome("Outro Atrasado").agora();
		List<Locacao> locacoes = Arrays.asList(
				umLocacao().comUsuario(usuario1).atrasado().agora(),
				umLocacao().comUsuario(usuario2).agora(), 
				umLocacao().comUsuario(usuario3).atrasado().agora(),
				umLocacao().comUsuario(usuario3).atrasado().agora());
		when(dao.obterLocacoesPendentes()).thenReturn(locacoes);

		// acao
		service.notificarAtrasos();

		// verificacao
		verify(emailService, Mockito.times(3)).notificarAtraso(Mockito.any(Usuario.class));
		verify(emailService).notificarAtraso(usuario1);
		verify(emailService, atLeastOnce()).notificarAtraso(usuario3);
		verify(emailService, Mockito.never()).notificarAtraso(usuario2);
		verifyNoMoreInteractions(emailService);
//		verifyZeroInteractions(spcService);
	}

	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws FilmeSemEstoqueException {
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		when(spcService.possuiNegativacao(Mockito.any(Usuario.class))).thenReturn(true);

//		exception.expectMessage("Usuário negativo");
//		exception.expect(LocadoraException.class);

		// acao
		try {
			service.alugarFilme(usuario, filmes);
			// verificacao
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usuário negativo"));
		}

//		verify(spcService).possuiNegativacao(usuario);

	}

	@Test
	public void deveAlugarFilme() throws Exception {
		Assume.assumeFalse(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));

		// cenario

		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());

		// acao
		Locacao locacao = service.alugarFilme(usuario, filmes);

		// verificacao
//		erro.checkThat(locacao.getValor(), is(equalTo(5.0)));
//		erro.checkThat(locacao.getValor(), is(not(6.0)));
//		assertEquals(5.0, locacao.getValor(), 0.01);
//		erro.checkThat(isMesmaData(locacao.getDataLocacao(), new Date()), is(true));
//		erro.checkThat(isMesmaData(locacao.getDataRetorno(), DataUtils.obterDataComDiferencaDias(1)), is(true));

		erro.checkThat(locacao.getDataLocacao(), ehHoje());
		erro.checkThat(locacao.getDataRetorno(), ehHojeComDiferencaDias(1));

		erro.checkThat(locacao.getValor(), is(equalTo(5.0)));
		erro.checkThat(locacao.getValor(), is(not(6.0)));
		assertEquals(5.0, locacao.getValor(), 0.01);
		erro.checkThat(isMesmaData(locacao.getDataLocacao(), new Date()), is(true));
		erro.checkThat(isMesmaData(locacao.getDataRetorno(), DataUtils.obterDataComDiferencaDias(1)), is(true));

	}

	@Test(expected = FilmeSemEstoqueException.class) // elegante
	public void naoDeveAlugarFilmeSemEstoque() throws Exception {
		// cenario
		List<Filme> filmes = Arrays.asList(umFilmeSemEstoque().agora());
		Usuario usuario = umUsuario().agora();

		// acao
		service.alugarFilme(usuario, filmes);
		System.out.println("Forma elegante");
	}

	@Test // Robusto
	public void naoDeveAlugarFilmeSemUsuario() throws FilmeSemEstoqueException {
		// cenarário
		List<Filme> filmes = Arrays.asList(umFilme().agora());
//		Usuario usuario = new Usuario("Usuario 1");

		// acao
		try {
			service.alugarFilme(null, filmes);
			Assert.fail();
		} catch (LocadoraException e) {
			assertThat(e.getMessage(), is("Usuário vazio"));
		}
		System.out.println("Forma Robusta");

	}

	// Forma Nova
	@Test
	public void naoDeveAlogarFilmeSemFilme() throws FilmeSemEstoqueException, LocadoraException {
		// cenarário
		Usuario usuario = umUsuario().agora();

		exception.expect(LocadoraException.class);
		exception.expectMessage("Filme vazio");
		// acao
		service.alugarFilme(usuario, null);

		System.out.println("Forma Nova");
	}

//	@Test
//	public void devePagar75PctNoFilme3() throws FilmeSemEstoqueException, LocadoraException {
//		// cenario
//		Usuario usuario = new Usuario("Usuario 1");
//		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 2, 4.0),
//				new Filme("Filme 3", 2, 4.0));
//
//		// acao
//		Locacao resultado = service.alugarFilme(usuario, filmes);
//
//		// verificacao
//		// 4+4+(3) - //4 = 11
//
//		assertThat(resultado.getValor(), is(11.0));
//
//	}
//
//	@Test
//	public void devePagar50PctNoFilme4() throws FilmeSemEstoqueException, LocadoraException {
//		// cenario
//		Usuario usuario = new Usuario("Usuario 1");
//		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 2, 4.0),
//				new Filme("Filme 3", 2, 4.0), new Filme("Filme 4", 2, 4.0));
//		// acao
//		Locacao resultado = service.alugarFilme(usuario, filmes);
//
//		// verificacao
//
//		// 4 +4+3+2 = 13
//		assertThat(resultado.getValor(), is(13.0));
//
//	}
//
//	@Test
//	public void devePagar25PctNoFilme5() throws FilmeSemEstoqueException, LocadoraException {
//		// cenario
//		Usuario usuario = new Usuario("Usuario 1");
//		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 2, 4.0),
//				new Filme("Filme 3", 2, 4.0), new Filme("Filme 4", 2, 4.0), new Filme("Filme 5", 2, 4.0));
//		// acao
//		Locacao resultado = service.alugarFilme(usuario, filmes);
//
//		// verificacao
//
//		// 4 +4+3+2 = 13
//		// 4 +4+3+2 +1 = 14
//		assertThat(resultado.getValor(), is(14.0));
//
//	}
//
//	@Test
//	public void devePagar0PctNoFilme6() throws FilmeSemEstoqueException, LocadoraException {
//		// cenario
//		Usuario usuario = new Usuario("Usuario 1");
//		List<Filme> filmes = Arrays.asList(new Filme("Filme 1", 2, 4.0), new Filme("Filme 2", 2, 4.0),
//				new Filme("Filme 3", 2, 4.0), new Filme("Filme 4", 2, 4.0), new Filme("Filme 5", 2, 4.0),
//				new Filme("Filme 6", 2, 4.0));
//		// acao
//		Locacao resultado = service.alugarFilme(usuario, filmes);
//
//		// verificacao
//
//		// 4 +4+3+2 = 13
//		// 4 +4+3+2 +1 = 14
//		assertThat(resultado.getValor(), is(14.0));
//
//	}

	@Test
//	@Ignore
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws FilmeSemEstoqueException, LocadoraException {
		Assume.assumeTrue(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		// acao
		Locacao retorno = service.alugarFilme(usuario, filmes);

		// verificacao
		boolean ehSegunda = DataUtils.verificarDiaSemana(retorno.getDataRetorno(), Calendar.MONDAY);
		Assert.assertTrue(ehSegunda);
//		assertThat(retorno.getDataRetorno(), new DiaSemanaMatcher(Calendar.MONDAY));
		assertThat(retorno.getDataRetorno(), caiEm(Calendar.MONDAY));

//		assertThat(retorno.getDataRetorno(), caiEm(Calendar.MONDAY));
		assertThat(retorno.getDataRetorno(), caiNumaSegunda());

	}
//
//	@Test() // Robusta
//	public void testeLocacao_FilmeSemEstoque2() {
//		// cenario
//		LocacaoService service = new LocacaoService();
//		Usuario usuario = new Usuario("Usuario 1");
//		Filme filme = new Filme("Filme 1", 0, 5.0);
//
//		// acao
//		try {
//			service.alugarFilme(usuario, filme);
//			Assert.fail("Deveria ter lançado uma exceção");
//		} catch (Exception e) {
//			Assert.assertThat(e.getMessage(), is("Filme sem estoque"));
//		}
//	}
//
//	@Test // Nova Forma
//	public void testeLocacao_FilmeSemEstoque3() throws Exception {
//		// cenario
//		LocacaoService service = new LocacaoService();
//		Usuario usuario = new Usuario("Usuario 1");
//		Filme filme = new Filme("Filme 1", 0, 5.0);
//
//		exception.expect(Exception.class);
//		exception.expectMessage("Filme sem estoque");
//		// acao
//		service.alugarFilme(usuario, filme);
//	}

	public static void main(String[] args) {

		new BuilderMaster().gerarCodigoClasse(Locacao.class);

	}

}
