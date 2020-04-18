package br.ce.wcaquino.servicos;

import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.FilmeBuilder.umFilmeSemEstoque;
import static br.ce.wcaquino.builders.LocacaoBuilder.umLocacao;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.DataDiferencaDiasMatchers.ehHoje;
import static br.ce.wcaquino.matchers.DataDiferencaDiasMatchers.ehHojeComDiferencaDias;
import static br.ce.wcaquino.matchers.MatchersProprios.caiNumaSegunda;
import static br.ce.wcaquino.utils.DataUtils.isMesmaData;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

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
	public ErrorCollector error = new ErrorCollector();

	@InjectMocks
	@Spy
	private LocacaoService service;

	@Mock
	private SPCService spcService;

	@Mock
	private LocacaoDao dao;

	@Mock
	private EmailService emailService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
//		service = PowerMockito.spy(service);
	}

	@Test // executa metodo privado diretamente
	public void deveCalcularValorLocacao() throws Exception {
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		// cenario

		// acao - reflect
		Class<LocacaoService> clazz = LocacaoService.class;
		Method metodo = clazz.getDeclaredMethod("calcularValorLocacao", List.class);
		metodo.setAccessible(true);
		Double valor = (Double) metodo.invoke(service, filmes);

//		Double valor = (Double) Whitebox.invokeMethod(service, "calcularValorLocacao", filmes); //powermock

		// verificacao
		Assert.assertThat(valor, is(4.0));

	}

//	@Test // moca metodo privado para teste
//	public void deveAlugarFilmeSemCalcularValor() throws Exception {
//
//		// cenario
//		Usuario usuario = umUsuario().agora();
//		List<Filme> filmes = Arrays.asList(umFilme().agora());
//
//		PowerMockito.doReturn(1.0).when(service, "calcularValorLocacao", filmes);
//
//		// acao
//		Locacao locacao = service.alugarFilme(usuario, filmes);
//
//		// verificacao
//		Assert.assertThat(locacao.getValor(), is(1.0));
//		PowerMockito.verifyPrivate(service).invoke("calcularValorLocacao", filmes);
//	}

	@Test
	public void deveProrrogarUmaLocacao() {
		// cenario
		Locacao locacao = umLocacao().agora();

		// acao
		service.prorrogarLocacao(locacao, 3);

		// verificacao
		// Mockito.verify(dao).salvar(Mockito.any(Locacao.class)); // new dentro do
		// metodo

		// argument captor para capturar o que foi passado.
		ArgumentCaptor<Locacao> argCaptor = ArgumentCaptor.forClass(Locacao.class);

		Mockito.verify(dao).salvar(argCaptor.capture());

		Locacao locacaoRetorno = argCaptor.getValue();

		error.checkThat(locacaoRetorno.getValor(), CoreMatchers.is(12.0));
//		Assert.assertThat(locacaoRetorno.getValor(), is(4.0));
		error.checkThat(locacaoRetorno.getDataLocacao(), ehHoje());
		error.checkThat(locacaoRetorno.getDataRetorno(), ehHojeComDiferencaDias(3));
	}

	@Test
	public void deveTratarErroNoSPC() throws Exception {
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		when(spcService.possuiNegativacao(usuario)).thenThrow(new Exception("Falha catastrófica"));

		exception.expect(LocadoraException.class);
		exception.expectMessage("Problemas com SPC, tente novamente!");

		// acao
		service.alugarFilme(usuario, filmes);

		// verificacao

	}

	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas() {
		// cenario
		Usuario usuario1 = umUsuario().agora();
		Usuario usuario2 = umUsuario().comNome("Usuario em dia").agora();
		Usuario usuario3 = umUsuario().comNome("Outro Atrasado").agora();
		List<Locacao> locacoes = Arrays.asList(umLocacao().comUsuario(usuario1).atrasado().agora(),
				umLocacao().comUsuario(usuario2).agora(), umLocacao().comUsuario(usuario3).atrasado().agora(),
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
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws Exception {
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
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());

		Mockito.doReturn(DataUtils.obterData(28, 4, 2017)).when(service).obterData();

		// acao
		Locacao locacao = service.alugarFilme(usuario, filmes);

		error.checkThat(isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(28, 4, 2017)), is(true));
		error.checkThat(isMesmaData(locacao.getDataRetorno(), DataUtils.obterData(29, 4, 2017)), is(true));

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
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception {
//		Assume.assumeTrue(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		Mockito.doReturn(DataUtils.obterData(29, 4, 2017)).when(service).obterData();

		// acao
		Locacao retorno = service.alugarFilme(usuario, filmes);

		// verificacao
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
